/**
 * Data Chart Component
 * Handles visualization of ETL data using Chart.js
 */
class DataChartManager {
    constructor() {
        // DOM elements
        this.chartContainer = document.getElementById('dataChart');
        this.chartMetricSelect = document.getElementById('chartMetric');
        this.chartGroupBySelect = document.getElementById('chartGroupBy');
        
        // Chart instance
        this.chart = null;
        
        // Data
        this.processedData = [];
        
        this.init();
    }
    
    /**
     * Initialize chart functionality
     */
    init() {
        // Create initial empty chart
        this.createChart();
        
        // Add event listeners for filter changes
        this.chartMetricSelect.addEventListener('change', () => {
            this.updateChart();
        });
        
        this.chartGroupBySelect.addEventListener('change', () => {
            this.updateChart();
        });
    }
    
    /**
     * Load data from API and update chart
     */
    async loadData() {
        try {
            // Get processed data
            this.processedData = await apiService.getAllData();
            
            // Update chart
            this.updateChart();
            
            // Update stats
            const stats = await apiService.getStatistics();
            this.updateStats(stats);
        } catch (error) {
            console.error('Error loading chart data:', error);
        }
    }
    
    /**
     * Create the initial chart
     */
    createChart() {
        const ctx = this.chartContainer.getContext('2d');
        
        this.chart = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: [],
                datasets: [{
                    label: 'No Data',
                    data: [],
                    backgroundColor: 'rgba(0, 102, 204, 0.6)',
                    borderColor: 'rgba(0, 102, 204, 1)',
                    borderWidth: 1
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'top',
                    },
                    title: {
                        display: true,
                        text: 'ETL Data Visualization'
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true
                    }
                }
            }
        });
    }
    
    /**
     * Update the chart with current data and filters
     */
    updateChart() {
        if (!this.processedData || this.processedData.length === 0) {
            return;
        }
        
        // Get selected metric and group by values
        const metric = this.chartMetricSelect.value;
        const groupBy = this.chartGroupBySelect.value;
        
        // Prepare chart data
        const chartData = this.prepareChartData(metric, groupBy);
        
        // Update chart
        this.chart.data.labels = chartData.labels;
        this.chart.data.datasets[0].label = this.getMetricLabel(metric);
        this.chart.data.datasets[0].data = chartData.values;
        
        // Update chart title
        this.chart.options.plugins.title.text = `${this.getMetricLabel(metric)} by ${this.getGroupByLabel(groupBy)}`;
        
        // Update chart colors
        this.chart.data.datasets[0].backgroundColor = this.generateColors(chartData.labels.length, 0.6);
        this.chart.data.datasets[0].borderColor = this.generateColors(chartData.labels.length, 1);
        
        // Refresh chart
        this.chart.update();
    }
    
    /**
     * Prepare data for the chart based on selected metric and grouping
     * @param {string} metric - The metric to display
     * @param {string} groupBy - The field to group by
     * @returns {Object} - Object with labels and values arrays
     */
    prepareChartData(metric, groupBy) {
        // Group data
        const groupedData = {};
        
        this.processedData.forEach(item => {
            let groupValue;
            
            // Handle different group by options
            switch (groupBy) {
                case 'country':
                    groupValue = item.country || 'Unknown';
                    break;
                case 'city':
                    groupValue = item.city || 'Unknown';
                    break;
                case 'age':
                    // Group ages into ranges
                    const age = item.age || 0;
                    if (age < 18) {
                        groupValue = 'Under 18';
                    } else if (age < 30) {
                        groupValue = '18-29';
                    } else if (age < 40) {
                        groupValue = '30-39';
                    } else if (age < 50) {
                        groupValue = '40-49';
                    } else if (age < 60) {
                        groupValue = '50-59';
                    } else {
                        groupValue = '60+';
                    }
                    break;
                default:
                    groupValue = 'All';
            }
            
            // Get metric value
            let metricValue = 0;
            switch (metric) {
                case 'age':
                    metricValue = item.age || 0;
                    break;
                case 'salary':
                    metricValue = item.salary || 0;
                    break;
                case 'netSalary':
                    metricValue = item.netSalary || 0;
                    break;
                case 'taxRate':
                    metricValue = item.taxRate || 0;
                    break;
                case 'dependents':
                    metricValue = item.dependents || 0;
                    break;
                default:
                    metricValue = 0;
            }
            
            // Add to grouped data
            if (!groupedData[groupValue]) {
                groupedData[groupValue] = {
                    sum: 0,
                    count: 0
                };
            }
            
            groupedData[groupValue].sum += metricValue;
            groupedData[groupValue].count++;
        });
        
        // Calculate averages or sums based on metric
        const useAverage = ['age', 'salary', 'netSalary', 'taxRate'].includes(metric);
        
        // Convert grouped data to arrays for chart
        const labels = [];
        const values = [];
        
        // Sort groups by value (descending)
        const sortedGroups = Object.keys(groupedData).sort((a, b) => {
            const valueA = useAverage ? 
                groupedData[a].sum / groupedData[a].count : 
                groupedData[a].sum;
            const valueB = useAverage ? 
                groupedData[b].sum / groupedData[b].count : 
                groupedData[b].sum;
            return valueB - valueA;
        });
        
        // Limit to top 10 groups
        const topGroups = sortedGroups.slice(0, 10);
        
        // Add to labels and values arrays
        topGroups.forEach(group => {
            labels.push(group);
            
            const value = useAverage ? 
                groupedData[group].sum / groupedData[group].count : 
                groupedData[group].sum;
            
            values.push(value);
        });
        
        return { labels, values };
    }
    
    /**
     * Get a human-readable label for a metric
     * @param {string} metric - The metric name
     * @returns {string} - Human-readable label
     */
    getMetricLabel(metric) {
        switch (metric) {
            case 'age': return 'Age';
            case 'salary': return 'Salary';
            case 'netSalary': return 'Net Salary';
            case 'taxRate': return 'Tax Rate';
            case 'dependents': return 'Dependents';
            default: return metric;
        }
    }
    
    /**
     * Get a human-readable label for a group by field
     * @param {string} groupBy - The group by field
     * @returns {string} - Human-readable label
     */
    getGroupByLabel(groupBy) {
        switch (groupBy) {
            case 'country': return 'Country';
            case 'city': return 'City';
            case 'age': return 'Age Range';
            default: return groupBy;
        }
    }
    
    /**
     * Generate an array of colors for chart
     * @param {number} count - Number of colors needed
     * @param {number} alpha - Alpha value for colors
     * @returns {Array} - Array of color strings
     */
    generateColors(count, alpha) {
        const colors = [];
        const baseColors = [
            [0, 102, 204],   // Primary blue
            [0, 204, 255],   // Secondary blue
            [0, 153, 204],   // Light blue
            [51, 102, 255],  // Royal blue
            [0, 204, 153],   // Teal
            [0, 153, 153],   // Dark teal
            [102, 102, 255], // Purple blue
            [51, 153, 255],  // Sky blue
            [0, 102, 153],   // Deep blue
            [51, 204, 204]   // Turquoise
        ];
        
        for (let i = 0; i < count; i++) {
            const colorIndex = i % baseColors.length;
            const [r, g, b] = baseColors[colorIndex];
            colors.push(`rgba(${r}, ${g}, ${b}, ${alpha})`);
        }
        
        return colors;
    }
    
    /**
     * Update statistics display
     * @param {Object} stats - Statistics object from API
     */
    updateStats(stats) {
        // Update stats elements
        document.getElementById('totalRecords').textContent = formatters.number(stats.totalRecords);
        document.getElementById('validRecords').textContent = formatters.number(stats.validRecords);
        document.getElementById('invalidRecords').textContent = formatters.number(stats.invalidRecords);
        document.getElementById('avgAge').textContent = formatters.number(stats.averageAge);
        document.getElementById('avgSalary').textContent = formatters.currency(stats.averageSalary);
        document.getElementById('avgNetSalary').textContent = formatters.currency(stats.averageNetSalary);
    }
}

// Initialize data chart when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.dataChart = new DataChartManager();
});
