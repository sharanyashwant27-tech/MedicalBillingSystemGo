async function generateReport() {
    const type = document.getElementById('reportType').value;
    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;
    try {
        const report = await API.get(`/api/reports?type=${type}&startDate=${startDate}&endDate=${endDate}`);
        document.getElementById('reportResult').innerHTML = '<pre class="bg-light p-3 rounded">' + JSON.stringify(report, null, 2) + '</pre>';
    } catch (e) { showToast(e.message, 'danger'); }
}

function exportReport(format) {
    const type = document.getElementById('reportType').value;
    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;
    window.open(`/api/reports/export?type=${type}&format=${format}&startDate=${startDate}&endDate=${endDate}`, '_blank');
}
