/**
 * Job Status Component
 * Handles tracking and displaying ETL job status with enhanced metrics
 * 
 * Features:
 * - Real-time job status monitoring
 * - Detailed step execution metrics
 * - Process duration tracking
 * - Thread usage information
 * - Metric explanations
 */
class JobStatusManager {
    constructor() {
        // DOM elements
        this.statusModal = document.getElementById('statusModal');
        this.jobStatusLoader = document.getElementById('jobStatusLoader');
        this.jobStatusContent = document.getElementById('jobStatusContent');
        this.jobIdElement = document.getElementById('jobId');
        this.jobStatusElement = document.getElementById('jobStatus');
        this.startTimeElement = document.getElementById('startTime');
        this.endTimeElement = document.getElementById('endTime');
        this.durationElement = document.getElementById('jobDuration');
        this.threadsElement = document.getElementById('threadsUsed');
        this.stepDetailsElement = document.getElementById('stepDetails');
        this.metricsExplanationElement = document.getElementById('metricsExplanation');
        this.closeStatusBtn = document.getElementById('closeStatusBtn');
        this.viewResultsBtn = document.getElementById('viewResultsBtn');
        
        // State
        this.currentJobId = null;
        this.pollingInterval = null;
        this.isCompleted = false;
        this.jobStartTime = null;
        this.jobEndTime = null;
        this.lastJobData = null;
        
        this.init();
    }
    
    /**
     * Initialize job status functionality
     */
    init() {
        // Close button event
        this.closeStatusBtn.addEventListener('click', () => {
            this.hideModal();
        });
        
        // View results button event
        this.viewResultsBtn.addEventListener('click', () => {
            this.hideModal();
            // Ensure the tab manager exists before switching
            if (window.tabManager) {
                window.tabManager.switchTab('result-tab');
                // Trigger data loading if the components exist
                if (window.resultsTable) window.resultsTable.loadData();
                if (window.dataChart) window.dataChart.loadData();
            } else {
                // Fallback to manual tab switching if tab manager doesn't exist
                const resultTab = document.getElementById('result-tab');
                const tabs = document.querySelectorAll('.tab-content');
                const navItems = document.querySelectorAll('.nav li');
                
                // Hide all tabs and remove active class from nav items
                tabs.forEach(tab => tab.classList.remove('active'));
                navItems.forEach(item => item.classList.remove('active'));
                
                // Show the results tab and set its nav item as active
                if (resultTab) {
                    resultTab.classList.add('active');
                    const resultNavItem = document.querySelector('.nav li[data-tab="result-tab"]');
                    if (resultNavItem) resultNavItem.classList.add('active');
                }
                
                // Manually trigger data loading
                if (window.resultsTable) window.resultsTable.loadData();
                if (window.dataChart) window.dataChart.loadData();
            }
        });
        
        // Close modal when clicking outside
        window.addEventListener('click', (e) => {
            if (e.target === this.statusModal) {
                this.hideModal();
            }
        });
        
        // Close modal with escape key
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && this.statusModal.classList.contains('active')) {
                this.hideModal();
            }
        });
        
        // Close button in modal header
        const closeModalBtn = document.querySelector('.close-modal');
        if (closeModalBtn) {
            closeModalBtn.addEventListener('click', () => {
                this.hideModal();
            });
        }
    }
    
    /**
     * Show the job status modal
     */
    showModal() {
        this.statusModal.classList.add('active');
        this.jobStatusLoader.classList.remove('hidden');
        this.jobStatusContent.classList.add('hidden');
        this.viewResultsBtn.disabled = true;
        this.isCompleted = false;
    }
    
    /**
     * Hide the job status modal
     */
    hideModal() {
        this.statusModal.classList.remove('active');
        this.stopStatusPolling();
    }
    
    /**
     * Update the job ID
     * @param {string} jobId - The job ID
     */
    updateJobId(jobId) {
        this.currentJobId = jobId;
        this.jobIdElement.textContent = jobId;
    }
    
    /**
     * Start polling for job status updates
     * @param {string} jobId - The job ID
     */
    startStatusPolling(jobId) {
        this.currentJobId = jobId;
        
        // Clear any existing interval
        this.stopStatusPolling();
        
        // Check status immediately
        this.checkJobStatus();
        
        // Start polling every 2 seconds
        this.pollingInterval = setInterval(() => {
            this.checkJobStatus();
        }, 2000);
    }
    
    /**
     * Stop polling for job status updates
     */
    stopStatusPolling() {
        if (this.pollingInterval) {
            clearInterval(this.pollingInterval);
            this.pollingInterval = null;
        }
    }
    
    /**
     * Check the current job status
     */
    checkJobStatus() {
        if (!this.currentJobId) {
            console.warn('No job ID available for status check');
            return;
        }
        
        fetch(`/api/etl/status/${this.currentJobId}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP error ${response.status}: ${response.statusText}`);
                }
                return response.json();
            })
            .then(data => {
                if (!data) {
                    console.warn(`Received empty status data for job ${this.currentJobId}`);
                    return;
                }
                
                // Store the raw job data for reference
                this.rawJobData = data;
                
                // Log the raw job data for debugging
                console.debug(`Job status data for ${this.currentJobId}:`, data);
                
                // Update UI with job status
                this.updateStatusUI(data);
                
                // Check if job is completed
                if (data.status === 'COMPLETED' || data.status === 'FAILED' || data.status === 'STOPPED') {
                    // Record the end time if not already set
                    if (!this.jobEndTime) {
                        this.jobEndTime = new Date().getTime();
                    }
                    
                    // Process job completion
                    this.jobCompleted(data.status);
                } else if (!this.jobStartTime) {
                    // Record the start time on first status check
                    this.jobStartTime = new Date().getTime();
                }
            })
            .catch(error => {
                console.error(`Error checking job status for ${this.currentJobId}:`, error);
                // Don't mark as completed on error, keep polling
            });
    }
    
    /**
     * Update the status UI with job details
     * @param {Object} status - The job status object
     */
    updateStatusUI(status) {
        // Store job data for history with enhanced metrics
        this.lastJobData = {
            ...status,
            fileName: status.fileName || this.getFileNameFromJobParameters(status),
            fileType: status.fileType || this.getFileTypeFromJobParameters(status),
            readCount: this.getTotalReadCount(status),
            writeCount: this.getTotalWriteCount(status),
            filterCount: this.getTotalFilterCount(status),
            skipCount: this.getTotalSkipCount(status)
        };
        
        // Show content, hide loader
        this.jobStatusLoader.classList.add('hidden');
        this.jobStatusContent.classList.remove('hidden');
        
        // Update status elements
        this.jobIdElement.textContent = status.jobId;
        this.jobStatusElement.textContent = status.status;
        
        // Add status color
        this.jobStatusElement.className = 'status-value';
        if (status.status === 'COMPLETED') {
            this.jobStatusElement.classList.add('status-success');
        } else if (status.status === 'FAILED' || status.status === 'STOPPED') {
            this.jobStatusElement.classList.add('status-error');
        } else if (status.status === 'STARTED' || status.status === 'STARTING') {
            this.jobStatusElement.classList.add('status-warning');
        }
        
        // Format times
        if (status.startTime) {
            this.jobStartTime = new Date(status.startTime);
            this.startTimeElement.textContent = formatters.dateTime(status.startTime);
        } else {
            this.startTimeElement.textContent = 'N/A';
        }
        
        if (status.endTime) {
            this.jobEndTime = new Date(status.endTime);
            this.endTimeElement.textContent = formatters.dateTime(status.endTime);
        } else {
            this.endTimeElement.textContent = 'N/A';
        }
        
        // Calculate and display duration
        if (this.jobStartTime) {
            const endTime = this.jobEndTime || new Date();
            const durationMs = endTime - this.jobStartTime;
            this.durationElement.textContent = this.formatDuration(durationMs);
        } else {
            this.durationElement.textContent = 'N/A';
        }
        
        // Display threads used
        if (status.threadsUsed) {
            this.threadsElement.textContent = status.threadsUsed;
        } else {
            // Default to configured threads from application properties
            this.threadsElement.textContent = status.maxThreads || 'N/A';
        }
        
        // Render step details
        this.renderStepDetails(status.steps || []);
        
        // Show metrics explanation
        this.showMetricsExplanation();
    }
    
    /**
     * Extract file name from job parameters
     * @param {Object} status - Job status object
     * @returns {string} File name
     */
    getFileNameFromJobParameters(status) {
        try {
            if (status.jobParameters && status.jobParameters.filePath) {
                const filePath = status.jobParameters.filePath.value || status.jobParameters.filePath;
                const pathParts = filePath.split(/[\\\/]/);
                return pathParts[pathParts.length - 1];
            }
        } catch (error) {
            console.error('Error extracting file name:', error);
        }
        return 'Unknown file';
    }
    
    /**
     * Extract file type from job parameters
     * @param {Object} status - Job status object
     * @returns {string} File type
     */
    getFileTypeFromJobParameters(status) {
        try {
            if (status.jobParameters && status.jobParameters.fileType) {
                return status.jobParameters.fileType.value || status.jobParameters.fileType;
            }
            // Try to extract from file name if available
            const fileName = this.getFileNameFromJobParameters(status);
            const extension = fileName.split('.').pop().toLowerCase();
            if (extension) {
                return extension;
            }
        } catch (error) {
            console.error('Error extracting file type:', error);
        }
        return 'Unknown type';
    }
    
    /**
     * Calculate total read count from all steps
     * @param {Object} status - Job status object
     * @returns {number} Total read count
     */
    getTotalReadCount(status) {
        try {
            if (status.steps && status.steps.length > 0) {
                return status.steps.reduce((total, step) => total + (step.readCount || 0), 0);
            }
        } catch (error) {
            console.error('Error calculating read count:', error);
        }
        return 0;
    }
    
    /**
     * Calculate total write count from all steps
     * @param {Object} status - Job status object
     * @returns {number} Total write count
     */
    getTotalWriteCount(status) {
        try {
            if (status.steps && status.steps.length > 0) {
                return status.steps.reduce((total, step) => total + (step.writeCount || 0), 0);
            }
        } catch (error) {
            console.error('Error calculating write count:', error);
        }
        return 0;
    }
    
    /**
     * Calculate total filter count from all steps
     * @param {Object} status - Job status object
     * @returns {number} Total filter count
     */
    getTotalFilterCount(status) {
        try {
            if (status.steps && status.steps.length > 0) {
                return status.steps.reduce((total, step) => total + (step.filterCount || 0), 0);
            }
        } catch (error) {
            console.error('Error calculating filter count:', error);
        }
        return 0;
    }
    
    /**
     * Calculate total skip count from all steps
     * @param {Object} status - Job status object
     * @returns {number} Total skip count
     */
    getTotalSkipCount(status) {
        try {
            if (status.steps && status.steps.length > 0) {
                const readSkips = status.steps.reduce((total, step) => total + (step.readSkipCount || 0), 0);
                const processSkips = status.steps.reduce((total, step) => total + (step.processSkipCount || 0), 0);
                const writeSkips = status.steps.reduce((total, step) => total + (step.writeSkipCount || 0), 0);
                return readSkips + processSkips + writeSkips;
            }
        } catch (error) {
            console.error('Error calculating skip count:', error);
        }
        return 0;
    }
    
    /**
     * Show explanation of ETL metrics
     */
    showMetricsExplanation() {
        if (!this.metricsExplanationElement) return;
        
        const explanations = {
            read: 'Number of records read from the source file.',
            write: 'Number of records successfully written to the database.',
            filter: 'Number of records excluded by processor logic. These records were read but intentionally not written.',
            skip: 'Number of records skipped due to errors or skip policies. This includes read, process, and write skips.',
            efficiency: 'Percentage of read records successfully written (Write Count ÷ Read Count × 100%).',
            duration: 'Total time taken to process the data.',
            threads: 'Number of concurrent threads used for processing.'
        };
        
        let explanationHtml = `
            <div class="metrics-explanation-card">
                <h4><i class="fas fa-info-circle"></i> ETL Metrics Explained</h4>
                <ul class="metrics-list">
        `;
        
        Object.entries(explanations).forEach(([metric, explanation]) => {
            explanationHtml += `
                <li>
                    <span class="metric-name">${metric.charAt(0).toUpperCase() + metric.slice(1)}:</span>
                    <span class="metric-explanation">${explanation}</span>
                </li>
            `;
        });
        
        explanationHtml += `
                </ul>
            </div>
        `;
        
        this.metricsExplanationElement.innerHTML = explanationHtml;
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
     * Render step execution details with enhanced metrics
     * @param {Array} steps - The step execution details
     */
    renderStepDetails(steps) {
        if (!steps || steps.length === 0) {
            this.stepDetailsElement.innerHTML = '<p class="no-data-message">No step details available</p>';
            return;
        }
        
        let stepsHtml = '';
        steps.forEach(step => {
            // Calculate metrics
            const readCount = step.readCount || 0;
            const writeCount = step.writeCount || 0;
            const filterCount = step.filterCount || 0;
            const skipCount = step.skipCount || 0;
            const processingEfficiency = readCount > 0 ? Math.round((writeCount / readCount) * 100) : 0;
            const stepStartTime = step.startTime ? new Date(step.startTime) : null;
            const stepEndTime = step.endTime ? new Date(step.endTime) : null;
            const durationMs = stepStartTime && stepEndTime ? stepEndTime - stepStartTime : 0;
            const durationText = this.formatDuration(durationMs);
            
            stepsHtml += `
            <div class="step-execution">
                <div class="step-header">
                    <h4 class="step-name">${step.stepName || 'Unknown Step'}</h4>
                    <span class="step-status ${step.status ? step.status.toLowerCase() : ''}">${step.status || 'UNKNOWN'}</span>
                </div>
                <div class="step-metrics">
                    <div class="metric">
                        <span class="metric-label">Read:</span>
                        <span class="metric-value">${readCount}</span>
                        <span class="metric-tooltip" title="Number of records read from the source"><i class="fas fa-info-circle"></i></span>
                    </div>
                    <div class="metric">
                        <span class="metric-label">Write:</span>
                        <span class="metric-value">${writeCount}</span>
                        <span class="metric-tooltip" title="Number of records successfully written to the database"><i class="fas fa-info-circle"></i></span>
                    </div>
                    <div class="metric">
                        <span class="metric-label">Filter:</span>
                        <span class="metric-value">${filterCount}</span>
                        <span class="metric-tooltip" title="Number of records excluded by processor logic"><i class="fas fa-info-circle"></i></span>
                    </div>
                    <div class="metric">
                        <span class="metric-label">Skip:</span>
                        <span class="metric-value">${skipCount}</span>
                        <span class="metric-tooltip" title="Number of records skipped due to errors or skip policies"><i class="fas fa-info-circle"></i></span>
                    </div>
                    <div class="metric">
                        <span class="metric-label">Efficiency:</span>
                        <span class="metric-value">${processingEfficiency}%</span>
                        <span class="metric-tooltip" title="Percentage of read records successfully written"><i class="fas fa-info-circle"></i></span>
                    </div>
                    <div class="metric">
                        <span class="metric-label">Duration:</span>
                        <span class="metric-value">${durationText}</span>
                    </div>
                </div>
            </div>
            `;
        });
        
        this.stepDetailsElement.innerHTML = stepsHtml;
    }
    
    /**
     * Handle job completion
     * @param {string} status - The final job status
     */
    jobCompleted(status) {
        try {
            // Prevent duplicate completion processing
            if (this.isCompleted) {
                console.log(`Job ${this.currentJobId} already marked as completed, ignoring duplicate completion event`);
                return;
            }
            
            console.log(`Processing job completion for ${this.currentJobId} with status: ${status}`);
            
            // Mark as completed and stop polling
            this.isCompleted = true;
            this.stopStatusPolling();
            
            // Enable view results button for both COMPLETED and FAILED statuses
            // This allows users to view results even when a job partially fails
            this.viewResultsBtn.disabled = !(status === 'COMPLETED' || status === 'FAILED');
            
            // Update button text based on status
            this.viewResultsBtn.textContent = status === 'COMPLETED' ? 'View Results' : 'View Partial Results';
            
            // Update close button text
            this.closeStatusBtn.textContent = 'Close';
            
            // Add job to history if completed or failed
            if (status === 'COMPLETED' || status === 'FAILED' || status === 'STOPPED') {
                // Ensure we have valid data to work with
                if (!this.lastJobData) {
                    console.warn(`No job data available for job ${this.currentJobId}, using defaults`);
                }
                
                // Extract file information from the raw job data or job parameters
                let fileName = 'Unknown file';
                let fileType = 'Unknown type';
                
                // Try to extract file information from job parameters if available
                if (this.rawJobData && this.rawJobData.jobParameters) {
                    const params = this.rawJobData.jobParameters;
                    
                    // Extract file path from job parameters
                    if (params.filePath && typeof params.filePath === 'string') {
                        // Extract just the filename from the path
                        const filePath = params.filePath.replace(/\{value=([^,]+).*\}/, '$1');
                        fileName = filePath.split('\\').pop(); // Get the last part after any backslashes
                    }
                    
                    // Extract file type from job parameters
                    if (params.fileType && typeof params.fileType === 'string') {
                        fileType = params.fileType.replace(/\{value=([^,]+).*\}/, '$1');
                    }
                }
                
                // Get step execution data if available
                let readCount = 0;
                let writeCount = 0;
                let filterCount = 0;
                let skipCount = 0;
                
                if (this.rawJobData && this.rawJobData.steps && this.rawJobData.steps.length > 0) {
                    // Use the last step's counts as they should have the final values
                    const lastStep = this.rawJobData.steps[this.rawJobData.steps.length - 1];
                    readCount = lastStep.readCount || 0;
                    writeCount = lastStep.writeCount || 0;
                    filterCount = lastStep.filterCount || 0;
                    skipCount = lastStep.skipCount || 0;
                }
                
                // Get job data for history with proper values
                const jobData = {
                    jobId: this.currentJobId,
                    status: status,
                    timestamp: new Date().toISOString(),
                    durationMs: this.jobEndTime && this.jobStartTime ? this.jobEndTime - this.jobStartTime : 0,
                    fileName: fileName,
                    fileType: fileType,
                    threadsUsed: this.lastJobData?.threadsUsed || this.rawJobData?.threadsUsed || 1,
                    readCount: readCount,
                    writeCount: writeCount,
                    filterCount: filterCount,
                    skipCount: skipCount
                };
                
                console.log('Job history data prepared:', jobData);
                
                // Add to history if the history component exists
                if (window.etlComponents?.jobHistory) {
                    console.log(`Adding job ${this.currentJobId} to history via direct component call`);
                    try {
                        window.etlComponents.jobHistory.addJobToHistory(jobData);
                    } catch (historyError) {
                        console.error('Error adding job to history component:', historyError);
                    }
                } else {
                    console.warn('Job history component not available, using event dispatch only');
                }
                
                // Always dispatch an event as a fallback mechanism
                try {
                    console.log(`Dispatching etl-job-completed event for job ${this.currentJobId}`);
                    const jobCompletedEvent = new CustomEvent('etl-job-completed', {
                        detail: jobData,
                        bubbles: true,
                        cancelable: true
                    });
                    document.dispatchEvent(jobCompletedEvent);
                } catch (eventError) {
                    console.error('Error dispatching job completion event:', eventError);
                }
                
                // Store job data in localStorage as an additional fallback
                try {
                    const storageKey = `etl-job-${this.currentJobId}`;
                    localStorage.setItem(storageKey, JSON.stringify({
                        ...jobData,
                        savedAt: new Date().toISOString()
                    }));
                    console.log(`Job data saved to localStorage with key: ${storageKey}`);
                } catch (storageError) {
                    console.error('Error saving job data to localStorage:', storageError);
                }
                
                console.log(`Job ${this.currentJobId} completion processing finished with status: ${status}`);
            } else {
                console.log(`Job ${this.currentJobId} has status ${status}, not adding to history`);
            }
        } catch (error) {
            console.error(`Unexpected error in jobCompleted for job ${this.currentJobId}:`, error);
        }
    }
}

// Initialize job status when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    // Initialize global ETL components if not already done
    window.etlComponents = window.etlComponents || {};
    // Add job status to global ETL components
    window.etlComponents.jobStatus = new JobStatusManager();
    // For backward compatibility
    window.jobStatus = window.etlComponents.jobStatus;
});
