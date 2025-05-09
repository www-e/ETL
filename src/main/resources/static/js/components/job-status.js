/**
 * Job Status Component
 * Handles tracking and displaying ETL job status
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
        this.stepDetailsElement = document.getElementById('stepDetails');
        this.closeStatusBtn = document.getElementById('closeStatusBtn');
        this.viewResultsBtn = document.getElementById('viewResultsBtn');
        
        // State
        this.currentJobId = null;
        this.pollingInterval = null;
        this.isCompleted = false;
        
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
            window.tabManager.switchTab('result-tab');
            window.resultsTable.loadData();
            window.dataChart.loadData();
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
    async checkJobStatus() {
        if (!this.currentJobId) return;
        
        try {
            const status = await apiService.getJobStatus(this.currentJobId);
            this.updateStatusUI(status);
            
            // Check if job is completed
            if (status.status === 'COMPLETED' || status.status === 'FAILED' || status.status === 'STOPPED') {
                this.jobCompleted(status.status);
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
        this.startTimeElement.textContent = status.startTime ? formatters.dateTime(status.startTime) : 'N/A';
        this.endTimeElement.textContent = status.endTime ? formatters.dateTime(status.endTime) : 'N/A';
        
        // Render step details
        this.renderStepDetails(status.steps || []);
    }
    
    /**
     * Render step execution details
     * @param {Array} steps - The step execution details
     */
    renderStepDetails(steps) {
        if (!steps || steps.length === 0) {
            this.stepDetailsElement.innerHTML = '<p class="no-data-message">No step details available</p>';
            return;
        }
        
        console.log('Step details received:', steps); // Debug log to check data
        
        let stepsHtml = '';
        steps.forEach(step => {
            // Add status class for coloring
            let statusClass = 'status-info';
            if (step.status === 'COMPLETED') statusClass = 'status-success';
            if (step.status === 'FAILED') statusClass = 'status-error';
            
            stepsHtml += `
                <div class="step-item">
                    <div class="step-header">
                        <h4>${step.stepName}</h4>
                        <span class="step-status ${statusClass}">${step.status}</span>
                    </div>
                    <div class="step-stats">
                        <div class="step-stat">
                            <span class="stat-label">Read:</span>
                            <span class="stat-value">${step.readCount}</span>
                        </div>
                        <div class="step-stat">
                            <span class="stat-label">Write:</span>
                            <span class="stat-value">${step.writeCount}</span>
                        </div>
                        <div class="step-stat">
                            <span class="stat-label">Filter:</span>
                            <span class="stat-value">${step.filterCount}</span>
                        </div>
                        <div class="step-stat">
                            <span class="stat-label">Skip:</span>
                            <span class="stat-value">${step.skipCount}</span>
                        </div>
                        <div class="step-stat">
                            <span class="stat-label">Commit:</span>
                            <span class="stat-value">${step.commitCount || 0}</span>
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
        if (this.isCompleted) return;
        
        this.isCompleted = true;
        this.stopStatusPolling();
        
        // Enable view results button if job completed successfully
        this.viewResultsBtn.disabled = status !== 'COMPLETED';
        
        // Update close button text
        this.closeStatusBtn.textContent = 'Close';
    }
}

// Initialize job status when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.jobStatus = new JobStatusManager();
});
