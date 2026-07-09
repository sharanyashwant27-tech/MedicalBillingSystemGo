document.addEventListener('DOMContentLoaded', function() {
    const ctx = document.getElementById('salesChart');
    if (!ctx) return;

    API.get('/api/dashboard').then(data => {
        new Chart(ctx, {
            type: 'bar',
            data: {
                labels: ['Today Sales', 'Today Profit', 'Medicines', 'Low Stock', 'Expired'],
                datasets: [{
                    label: 'Dashboard Metrics',
                    data: [
                        parseFloat(data.todaySales) || 0,
                        parseFloat(data.todayProfit) || 0,
                        data.availableMedicines || 0,
                        data.lowStockMedicines || 0,
                        data.expiredMedicines || 0
                    ],
                    backgroundColor: [
                        'rgba(25, 135, 84, 0.7)',
                        'rgba(13, 202, 240, 0.7)',
                        'rgba(111, 66, 193, 0.7)',
                        'rgba(255, 193, 7, 0.7)',
                        'rgba(220, 53, 69, 0.7)'
                    ],
                    borderRadius: 8
                }]
            },
            options: {
                responsive: true,
                plugins: { legend: { display: false } },
                scales: { y: { beginAtZero: true } }
            }
        });
    }).catch(console.error);
});
