async function saveBranch() {
    const data = {
        branchCode: document.getElementById('branchCode').value,
        branchName: document.getElementById('branchName').value,
        address: document.getElementById('branchAddress').value,
        city: document.getElementById('branchCity').value,
        phone: document.getElementById('branchPhone').value,
        active: true
    };
    try {
        await API.post('/api/branches', data);
        showToast('Branch created');
        location.reload();
    } catch (e) { showToast(e.message, 'danger'); }
}
