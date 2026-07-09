async function updateStatus(orderId, status) {
    try {
        await API.put('/api/online-orders/' + orderId + '/status', { status });
        showToast('Order status updated to ' + status);
        location.reload();
    } catch (e) { showToast(e.message, 'danger'); }
}
