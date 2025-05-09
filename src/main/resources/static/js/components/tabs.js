/**
 * Tab navigation component
 * Handles switching between different tabs in the UI
 */
class TabManager {
    constructor() {
        this.tabs = document.querySelectorAll('.nav li');
        this.tabContents = document.querySelectorAll('.tab-content');
        this.activeTab = 'input-tab'; // Default active tab
        
        this.init();
        
        // Ensure only one tab is visible on page load
        this.switchTab(this.activeTab);
    }
    
    /**
     * Initialize tab functionality
     */
    init() {
        // Add click event listeners to tabs
        this.tabs.forEach(tab => {
            tab.addEventListener('click', () => {
                const tabId = tab.getAttribute('data-tab');
                this.switchTab(tabId);
            });
        });
    }
    
    /**
     * Switch to a specific tab
     * @param {string} tabId - The ID of the tab to switch to
     */
    switchTab(tabId) {
        // Update active tab
        this.activeTab = tabId;
        
        // Update tab UI
        this.tabs.forEach(tab => {
            if (tab.getAttribute('data-tab') === tabId) {
                tab.classList.add('active');
            } else {
                tab.classList.remove('active');
            }
        });
        
        // Hide all tab content first
        this.tabContents.forEach(content => {
            content.classList.remove('active');
            content.style.display = 'none';
        });
        
        // Show only the active tab content
        const activeContent = document.getElementById(tabId);
        if (activeContent) {
            activeContent.classList.add('active');
            activeContent.style.display = 'block';
        }
    }
}

// Initialize tabs when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.tabManager = new TabManager();
});
