/**
 * Enhanced Job Status Component
 * Handles tracking and displaying ETL job status with detailed metrics
 * 
 * Features:
 * - Real-time job status monitoring
 * - Detailed step execution metrics with explanations
 * - Process duration tracking
 * - Thread usage information
 * - Integration with job history
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
        this.fileName = null;
        this.fileType = null;
        
        this.init();
    }
    
    /**
     * Initialize job status functionality
     */
    init() {
        // Close button event
        if (this.closeStatusBtn) {
            this.closeStatusBtn.addEventListener('click', () => {
                this.hideModal();
            });
        }
        
        // View results button event
        if (this.viewResultsBtn) {
            this.viewResultsBtn.addEventListener('click', () => {
                this.hideModal();
                // Switch to results tab
                const resultsTab = document.querySelector('a[href="#results"]');
                if (resultsTab) {
                    resultsTab.click();
                }
                
                // Refresh results if needed
                if (window.etlComponents && window.etlComponents.resultsTable) {
                    window.etlComponents.resultsTable.loadData();
                }
            });
        }
        
        // Close modal when clicking outside
        window.addEventListener('click', (e) => {
            if (e.target === this.statusModal) {
                this.hideModal();
            }
        });
        
        // Close modal with escape key
        window.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && this.statusModal.classList.contains('show')) {
                this.hideModal();
            }
        });
    }
    
    /**
     * Show the job status modal
     */
    showModal() {
        if (this.statusModal) {
            this.statusModal.classList.add('show');
            document.body.classList.add('modal-open');
        }
    }
    
    /**
     * Hide the job status modal
     */
    hideModal() {
        if (this.statusModal) {
            this.statusModal.classList.remove('show');
            document.body.classList.remove('modal-open');
        }
    }
    
    /**
     * Track a new job
     * @param {string} jobId - The job ID
     * @param {string} fileName - The file name
     * @param {string} fileType - The file type
     */
    trackJob(jobId, fileName, fileType) {
        this.currentJobId = jobId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.isCompleted = false;
        this.jobStartTime = new Date();
        this.jobEndTime = null;
        
        // Reset UI
        this.resetUI();
        
        // Show modal
        this.showModal();
        
        // Start polling
        this.startStatusPolling(jobId);
    }
    
    /**
     * Reset the UI for a new job
     */
    resetUI() {
        // Show loader, hide content
        if (this.jobStatusLoader) this.jobStatusLoader.classList.remove('hidden');
        if (this.jobStatusContent) this.jobStatusContent.classList.add('hidden');
        
        // Reset elements
        if (this.jobIdElement) this.jobIdElement.textContent = this.currentJobId || 'Loading...';
        if (this.jobStatusElement) {
            this.jobStatusElement.textContent = 'STARTING';
            this.jobStatusElement.className = 'status-value status-warning';
        }
        if (this.startTimeElement) this.startTimeElement.textContent = formatters.dateTime(new Date());
        if (this.endTimeElement) this.endTimeElement.textContent = 'N/A';
        if (this.durationElement) this.durationElement.textContent = 'Calculating...';
        if (this.threadsElement) this.threadsElement.textContent = 'Loading...';
        if (this.stepDetailsElement) this.stepDetailsElement.innerHTML = '<div class="loading-indicator">Loading step details...</div>';
        if (this.metricsExplanationElement) this.metricsExplanationElement.innerHTML = '';
    }
    
    /**
     * Update the job ID display
     * @param {string} jobId - The job ID
     */
    updateJobId(jobId) {
        this.currentJobId = jobId;
        if (this.jobIdElement) this.jobIdElement.textContent = jobId;
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
    async checkJobStatus() {
        if (!this.currentJobId) return;
        
        try {
            const status = await apiService.getJobStatus(this.currentJobId);
            this.updateStatusUI(status);
            
            // Check if job is completed
            if (status.status === 'COMPLETED' || status.status === 'FAILED' || status.status === 'STOPPED') {
                this.jobCompleted(status);
            }
        } catch (error) {
            console.error('Error checking job status:', error);
        }
    }
    
    /**
     * Update the status UI with job details
     * @param {Object} status - The job status object
     */
    updateStatusUI(status) {
        // Store job data for history
        this.lastJobData = status;
        
        // Show content, hide loader
        if (this.jobStatusLoader) this.jobStatusLoader.classList.add('hidden');
        if (this.jobStatusContent) this.jobStatusContent.classList.remove('hidden');
        
        // Update status elements
        if (this.jobIdElement) this.jobIdElement.textContent = status.jobId || this.currentJobId;
        
        if (this.jobStatusElement) {
            this.jobStatusElement.textContent = status.status || 'UNKNOWN';
            
            // Add status color
            this.jobStatusElement.className = 'status-value';
            if (status.status === 'COMPLETED') {
                this.jobStatusElement.classList.add('status-success');
            } else if (status.status === 'FAILED' || status.status === 'STOPPED') {
                this.jobStatusElement.classList.add('status-error');
            } else if (status.status === 'STARTED' || status.status === 'STARTING') {
                this.jobStatusElement.classList.add('status-warning');
            }
        }
        
        // Format times
        if (status.startTime) {
            this.jobStartTime = new Date(status.startTime);
            if (this.startTimeElement) this.startTimeElement.textContent = formatters.dateTime(status.startTime);
        } else if (this.startTimeElement) {
            this.startTimeElement.textContent = 'N/A';
        }
        
        if (status.endTime) {
            this.jobEndTime = new Date(status.endTime);
            if (this.endTimeElement) this.endTimeElement.textContent = formatters.dateTime(status.endTime);
        } else if (this.endTimeElement) {
            this.endTimeElement.textContent = 'N/A';
        }
        
        // Calculate and display duration
        if (this.jobStartTime && this.durationElement) {
            const endTime = this.jobEndTime || new Date();
            const durationMs = endTime - this.jobStartTime;
            this.durationElement.textContent = this.formatDuration(durationMs);
        } else if (this.durationElement) {
            this.durationElement.textContent = 'N/A';
        }
        
        // Display threads used
        if (this.threadsElement) {
            if (status.threadsUsed) {
                this.threadsElement.textContent = status.threadsUsed;
            } else {
                // Default to configured threads from application properties
                this.threadsElement.textContent = status.maxThreads || 'N/A';
            }
        }
        
        // Render step details
        this.renderStepDetails(status.steps || []);
        
        // Show metrics explanation
        this.showMetricsExplanation();
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
        if (!this.stepDetailsElement) return;
        
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
            const processingEfficiency = readCount > 0 ? ((writeCount / readCount) * 100).toFixed(1) : 0;
            
            // Add status class for coloring
            let statusClass = 'status-info';
            if (step.status === 'COMPLETED') statusClass = 'status-success';
            else if (step.status === 'FAILED') statusClass = 'status-error';
            
            // Calculate step duration if available
            let durationText = 'N/A';
            if (step.startTime && step.endTime) {
                const startTime = new Date(step.startTime);
                const endTime = new Date(step.endTime);
                const durationMs = endTime - startTime;
                durationText = this.formatDuration(durationMs);
            }
            
            stepsHtml += `
                <div class="step-detail">
                    <div class="step-header">
                        <span class="step-name">${step.stepName || 'Unknown Step'}</span>
                        <span class="step-status ${statusClass}">${step.status || 'UNKNOWN'}</span>
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
     * Show metrics explanation
     */
    showMetricsExplanation() {
        if (!this.metricsExplanationElement) return;
        
        const explanationHtml = `
            <div class="metrics-explanation">
                <h4><i class="fas fa-info-circle"></i> Understanding ETL Metrics</h4>
                <div class="explanation-content">
                    <p><strong>Read</strong>: The number of records read from the source file.</p>
                    <p><strong>Write</strong>: The number of records successfully written to the database after processing.</p>
                    <p><strong>Filter</strong>: The number of records that were excluded during processing due to not meeting criteria or validation rules. These records were read but intentionally not written.</p>
                    <p><strong>Skip</strong>: The number of records that were skipped due to errors during reading, processing, or writing phases. Unlike filtered records, skipped records typically represent unexpected issues.</p>
                    <p><strong>Efficiency</strong>: The percentage of read records that were successfully written (Write ÷ Read × 100%).</p>
                </div>
            </div>
        `;
        
        this.metricsExplanationElement.innerHTML = explanationHtml;
    }
    
    /**
     * Handle job completion
     * @param {Object} status - The final job status
     */
    jobCompleted(status) {
        if (this.isCompleted) return;
        
        // Stop polling
        this.stopStatusPolling();
        this.isCompleted = true;
        this.jobEndTime = new Date();
        
        console.log('Job completed with status:', status.status);
        
        // Add to history if available
        this.addToHistory(status);
    }
    
    /**
     * Add job to history
     * @param {Object} status - The job status
     */
    addToHistory(status) {
        // Check if history component is available
        if (!window.etlComponents || !window.etlComponents.jobHistory) return;
        
        // Create history entry
        const historyEntry = {
            jobId: this.currentJobId,
            fileName: this.fileName || 'Unknown file',
            fileType: this.fileType || 'Unknown type',
            status: status.status,
            timestamp: new Date().toISOString(),
            durationMs: this.jobEndTime - this.jobStartTime,
            threadsUsed: status.threadsUsed || status.maxThreads || 'N/A',
            readCount: 0,
            writeCount: 0,
            filterCount: 0,
            skipCount: 0
        };
        
        // Add step metrics
        if (status.steps && status.steps.length > 0) {
            const mainStep = status.steps[0];
            historyEntry.readCount = mainStep.readCount || 0;
            historyEntry.writeCount = mainStep.writeCount || 0;
            historyEntry.filterCount = mainStep.filterCount || 0;
            historyEntry.skipCount = mainStep.skipCount || 0;
        }
        
        // Add to history
        window.etlComponents.jobHistory.addJobToHistory(historyEntry);
    }
}

// Initialize on load
document.addEventListener('DOMContentLoaded', () => {
    // Add to global ETL components
    window.etlComponents = window.etlComponents || {};
    window.etlComponents.jobStatus = new JobStatusManager();
});
