/**
 * Job History Component
 * 
 * Manages the ETL job history, including:
 * - Storing completed job information
 * - Displaying job history in a collapsible card format
 * - Linking to specific job results
 */
class JobHistory {
    constructor() {
        // DOM elements
        this.historyContainer = document.getElementById('jobHistoryContainer');
        this.historyContent = document.getElementById('historyContent');
        this.clearHistoryBtn = document.getElementById('clearHistoryBtn');
        this.historyLoader = document.getElementById('historyLoader');
        
        // Initialize history from localStorage
        this.history = this.loadHistory();
        
        // Event listeners
        if (this.clearHistoryBtn) {
            this.clearHistoryBtn.addEventListener('click', () => this.clearHistory());
        }
        
        // Initialize
        this.renderHistory();
    }
    
    /**
     * Load job history from localStorage
     */
    loadHistory() {
        try {
            // Load main history from localStorage
            const savedHistory = localStorage.getItem('etlJobHistory');
            let history = savedHistory ? JSON.parse(savedHistory) : [];
            
            // Check if we need to recover any jobs from fallback storage
            const recoveredJobs = this.recoverJobsFromFallbackStorage();
            if (recoveredJobs.length > 0) {
                console.log(`Recovered ${recoveredJobs.length} jobs from fallback storage`);
                
                // Merge recovered jobs with existing history, avoiding duplicates
                recoveredJobs.forEach(recoveredJob => {
                    // Check if job already exists in history
                    const existingIndex = history.findIndex(job => job.jobId === recoveredJob.jobId);
                    if (existingIndex === -1) {
                        // Job doesn't exist in history, add it
                        history.unshift(recoveredJob);
                    }
                });
                
                // Sort history by timestamp (newest first)
                history.sort((a, b) => {
                    const dateA = new Date(a.timestamp);
                    const dateB = new Date(b.timestamp);
                    return dateB - dateA;
                });
            }
            
            return history;
        } catch (error) {
            console.error('Error loading job history:', error);
            return [];
        }
    }
    
    /**
     * Recover jobs from fallback localStorage storage
     * @returns {Array} Recovered job data
     */
    recoverJobsFromFallbackStorage() {
        const recoveredJobs = [];
        try {
            // Look for job data stored with the pattern etl-job-{jobId}
            for (let i = 0; i < localStorage.length; i++) {
                const key = localStorage.key(i);
                if (key && key.startsWith('etl-job-')) {
                    try {
                        const jobData = JSON.parse(localStorage.getItem(key));
                        if (jobData && jobData.jobId) {
                            recoveredJobs.push(jobData);
                            console.log(`Recovered job ${jobData.jobId} from fallback storage`);
                            
                            // Remove the fallback storage item to avoid duplicates in the future
                            localStorage.removeItem(key);
                        }
                    } catch (parseError) {
                        console.error(`Error parsing job data from key ${key}:`, parseError);
                    }
                }
            }
        } catch (error) {
            console.error('Error recovering jobs from fallback storage:', error);
        }
        return recoveredJobs;
    }
    
    /**
     * Save job history to localStorage
     */
    saveHistory() {
        try {
            // Limit history to most recent 50 jobs to prevent localStorage overflow
            const limitedHistory = this.history.slice(0, 50);
            localStorage.setItem('etlJobHistory', JSON.stringify(limitedHistory));
        } catch (error) {
            console.error('Error saving job history:', error);
        }
    }
    
    /**
     * Add a new job to history
     * @param {Object} jobData - Job execution data
     */
    addJobToHistory(jobData) {
        try {
            // Validate job data
            if (!jobData || typeof jobData !== 'object') {
                console.error('Invalid job data provided to addJobToHistory:', jobData);
                return;
            }
            
            // Check for required fields
            if (!jobData.jobId) {
                console.warn('Job data missing jobId, generating a fallback ID');
                jobData.jobId = 'job-' + Date.now();
            }
            
            // Add timestamp if not present
            if (!jobData.timestamp) {
                jobData.timestamp = new Date().toISOString();
            }
            
            // Ensure all numeric fields are valid numbers
            ['readCount', 'writeCount', 'filterCount', 'skipCount', 'durationMs', 'threadsUsed'].forEach(field => {
                if (jobData[field] === undefined || jobData[field] === null || isNaN(jobData[field])) {
                    jobData[field] = 0;
                }
            });
            
            // Ensure status is a valid string
            if (!jobData.status || typeof jobData.status !== 'string') {
                jobData.status = 'UNKNOWN';
            }
            
            // Ensure fileName and fileType are valid strings
            if (!jobData.fileName || typeof jobData.fileName !== 'string') {
                // Try to extract from job parameters if available
                if (jobData.jobParameters && jobData.jobParameters.filePath) {
                    const filePath = jobData.jobParameters.filePath.toString();
                    // Extract filename from path - handle both formats
                    if (filePath.includes('{value=')) {
                        // Handle Spring batch parameter format
                        const match = filePath.match(/\{value=([^,]+)/);
                        if (match && match[1]) {
                            const path = match[1];
                            jobData.fileName = path.split(/[\\\/]/).pop();
                        }
                    } else {
                        // Handle regular path format
                        jobData.fileName = filePath.split(/[\\\/]/).pop();
                    }
                }
                
                // Fallback if extraction failed
                if (!jobData.fileName || typeof jobData.fileName !== 'string') {
                    jobData.fileName = 'Unknown file';
                }
            }
            
            if (!jobData.fileType || typeof jobData.fileType !== 'string') {
                // Try to extract from job parameters if available
                if (jobData.jobParameters && jobData.jobParameters.fileType) {
                    const fileType = jobData.jobParameters.fileType.toString();
                    if (fileType.includes('{value=')) {
                        // Handle Spring batch parameter format
                        const match = fileType.match(/\{value=([^,]+)/);
                        if (match && match[1]) {
                            jobData.fileType = match[1];
                        }
                    } else {
                        jobData.fileType = fileType;
                    }
                } else if (jobData.fileName && jobData.fileName !== 'Unknown file') {
                    // Try to extract from file name
                    const parts = jobData.fileName.split('.');
                    if (parts.length > 1) {
                        jobData.fileType = parts[parts.length - 1].toLowerCase();
                    }
                }
                
                // Fallback if extraction failed
                if (!jobData.fileType || typeof jobData.fileType !== 'string') {
                    jobData.fileType = 'Unknown type';
                }
            }
            
            // Check for duplicate job ID and remove if exists
            const existingIndex = this.history.findIndex(job => job.jobId === jobData.jobId);
            if (existingIndex !== -1) {
                console.log(`Updating existing job in history: ${jobData.jobId}`);
                this.history.splice(existingIndex, 1);
            }
            
            // Add to beginning of array (newest first)
            this.history.unshift(jobData);
            
            // Save and update UI
            this.saveHistory();
            this.renderHistory();
            
            console.log(`Job ${jobData.jobId} successfully added to history`);
        } catch (error) {
            console.error('Error adding job to history:', error);
        }
    }
    
    /**
     * Clear all job history
     */
    clearHistory() {
        if (confirm('Are you sure you want to clear all job history?')) {
            this.history = [];
            this.saveHistory();
            this.renderHistory();
        }
    }
    
    /**
     * Format date for display
     * @param {string} dateString - ISO date string
     * @returns {string} Formatted date
     */
    formatDate(dateString) {
        try {
            const date = new Date(dateString);
            return date.toLocaleString();
        } catch (error) {
            return dateString || 'Unknown';
        }
    }
    
    /**
     * Format duration for display
     * @param {number} durationMs - Duration in milliseconds
     * @returns {string} Formatted duration
     */
    formatDuration(durationMs) {
        if (!durationMs && durationMs !== 0) return 'N/A';
        
        if (durationMs < 1000) {
            return `${durationMs}ms`;
        } else if (durationMs < 60000) {
            return `${(durationMs / 1000).toFixed(2)}s`;
        } else {
            const minutes = Math.floor(durationMs / 60000);
            const seconds = ((durationMs % 60000) / 1000).toFixed(0);
            return `${minutes}m ${seconds}s`;
        }
    }
    
    /**
     * Get status badge HTML
     * @param {string} status - Job status
     * @returns {string} HTML for status badge
     */
    getStatusBadge(status) {
        if (!status) return '<span class="badge badge-secondary">Unknown</span>';
        
        const statusLower = status.toLowerCase();
        let badgeClass = 'badge-secondary';
        
        if (statusLower === 'completed') {
            badgeClass = 'badge-success';
        } else if (statusLower === 'failed') {
            badgeClass = 'badge-danger';
        } else if (statusLower === 'running') {
            badgeClass = 'badge-primary';
        } else if (statusLower === 'stopped') {
            badgeClass = 'badge-warning';
        }
        
        return `<span class="badge ${badgeClass}">${status}</span>`;
    }
    
    /**
     * Render job history in the UI
     */
    renderHistory() {
        if (!this.historyContent) return;
        
        if (!this.history || this.history.length === 0) {
            this.historyContent.innerHTML = '<p class="no-data-message">No job history available</p>';
            return;
        }
        
        // Show content, hide loader
        if (this.historyLoader) this.historyLoader.classList.add('hidden');
        this.historyContent.classList.remove('hidden');
        
        const historyHTML = this.history.map((job, index) => {
            const jobId = job.jobId || `job-${index}`;
            const fileName = job.fileName || 'Unknown file';
            const fileType = job.fileType || 'Unknown type';
            const status = job.status || 'Unknown';
            const timestamp = this.formatDate(job.timestamp);
            const duration = this.formatDuration(job.durationMs);
            const threadsUsed = job.threadsUsed || 'N/A';
            const readCount = job.readCount || 0;
            const writeCount = job.writeCount || 0;
            const filterCount = job.filterCount || 0;
            const skipCount = job.skipCount || 0;
            
            return `
                <div class="history-card" data-job-id="${jobId}">
                    <div class="history-card-header" onclick="toggleHistoryCard(this)">
                        <div class="history-card-title">
                            <i class="fas fa-file-alt"></i>
                            <span>${fileName}</span>
                        </div>
                        <div class="history-card-status">
                            ${this.getStatusBadge(status)}
                        </div>
                        <div class="history-card-toggle">
                            <i class="fas fa-chevron-down"></i>
                        </div>
                    </div>
                    <div class="history-card-body">
                        <div class="history-card-details">
                            <div class="detail-row">
                                <div class="detail-item">
                                    <span class="detail-label">File Type:</span>
                                    <span class="detail-value">${fileType}</span>
                                </div>
                                <div class="detail-item">
                                    <span class="detail-label">Processed At:</span>
                                    <span class="detail-value">${timestamp}</span>
                                </div>
                            </div>
                            <div class="detail-row">
                                <div class="detail-item">
                                    <span class="detail-label">Duration:</span>
                                    <span class="detail-value">${duration}</span>
                                </div>
                                <div class="detail-item">
                                    <span class="detail-label">Threads Used:</span>
                                    <span class="detail-value">${threadsUsed}</span>
                                </div>
                            </div>
                            <div class="detail-row">
                                <div class="detail-item">
                                    <span class="detail-label">Read:</span>
                                    <span class="detail-value">${readCount}</span>
                                </div>
                                <div class="detail-item">
                                    <span class="detail-label">Written:</span>
                                    <span class="detail-value">${writeCount}</span>
                                </div>
                                <div class="detail-item">
                                    <span class="detail-label">Filtered:</span>
                                    <span class="detail-value">${filterCount}</span>
                                </div>
                                <div class="detail-item">
                                    <span class="detail-label">Skipped:</span>
                                    <span class="detail-value">${skipCount}</span>
                                </div>
                            </div>
                        </div>
                        <div class="history-card-actions">
                            <button class="btn btn-sm btn-primary" onclick="viewJobResults('${jobId}')">
                                <i class="fas fa-table"></i> View Results
                            </button>
                        </div>
                    </div>
                </div>
            `;
        }).join('');
        
        this.historyContent.innerHTML = historyHTML || '<p class="no-data-message">No job history available</p>';
    }
}

// Global function to toggle history card expansion
function toggleHistoryCard(headerElement) {
    const card = headerElement.closest('.history-card');
    card.classList.toggle('expanded');
    
    const toggleIcon = headerElement.querySelector('.history-card-toggle i');
    if (card.classList.contains('expanded')) {
        toggleIcon.classList.remove('fa-chevron-down');
        toggleIcon.classList.add('fa-chevron-up');
    } else {
        toggleIcon.classList.remove('fa-chevron-up');
        toggleIcon.classList.add('fa-chevron-down');
    }
}

// Global function to view job results
function viewJobResults(jobId) {
    // Switch to results tab and filter by job ID
    const resultsTab = document.querySelector('li[data-tab="result-tab"]');
    if (resultsTab) {
        // Activate the results tab
        document.querySelectorAll('.nav li').forEach(tab => tab.classList.remove('active'));
        resultsTab.classList.add('active');
        
        // Show the results content
        document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));
        document.getElementById('result-tab').classList.add('active');
        
        // Load the job data if available
        const apiService = window.etlComponents?.apiService;
        if (apiService && typeof apiService.loadJobResults === 'function') {
            apiService.loadJobResults(jobId);
        }
    }
}

// Initialize on load
document.addEventListener('DOMContentLoaded', () => {
    // Add to global ETL components
    window.etlComponents = window.etlComponents || {};
    window.etlComponents.jobHistory = new JobHistory();
    
    // Listen for job completion events from the job status component
    document.addEventListener('etl-job-completed', (event) => {
        if (event.detail && window.etlComponents.jobHistory) {
            window.etlComponents.jobHistory.addJobToHistory(event.detail);
        }
    });
});
