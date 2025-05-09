/**
 * Main Application
 * Initializes and coordinates all ETL application components
 */
class EtlApp {
    constructor() {
        // Initialize state
        this.isProcessing = false;
        
        // Initialize components
        this.initComponents();
        
        // Add global event listeners
        this.addEventListeners();
        
        console.log('ETL Application initialized');
    }
    
    /**
     * Initialize application components
     * Components are initialized in their own files, but we can access them via window
     */
    initComponents() {
        // Components are initialized in their own files
        // We can access them via window.componentName
        this.tabManager = window.tabManager;
        this.fileUpload = window.fileUpload;
        this.dataPreview = window.dataPreview;
        this.jobStatus = window.jobStatus;
        this.resultsTable = window.resultsTable;
        this.dataChart = window.dataChart;
    }
    
    /**
     * Add global event listeners
     */
    addEventListeners() {
        // Add any global event listeners here
        document.addEventListener('keydown', (e) => {
            // Add keyboard shortcuts if needed
        });
        
        // Listen for custom events
        document.addEventListener('etl:jobCompleted', (e) => {
            this.handleJobCompleted(e.detail);
        });
    }
    
    /**
     * Handle job completion
     * @param {Object} jobDetails - Details about the completed job
     */
    handleJobCompleted(jobDetails) {
        if (jobDetails && jobDetails.status === 'COMPLETED') {
            // Load results data
            this.resultsTable.loadData();
            this.dataChart.loadData();
        }
    }
}

// Initialize application when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.etlApp = new EtlApp();
});
