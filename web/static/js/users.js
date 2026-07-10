function openUserModal() {
    document.getElementById('userId').value = '';
    document.getElementById('username').value = '';
    document.getElementById('password').value = '';
    document.getElementById('fullName').value = '';
    document.getElementById('email').value = '';
    document.getElementById('role').value = 'ROLE_CASHIER';
}

async function saveUser() {
    const data = {
        username: document.getElementById('username').value,
        password: document.getElementById('password').value,
        fullName: document.getElementById('fullName').value,
        email: document.getElementById('email').value,
        roles: [document.getElementById('role').value]
    };
    try {
        await API.post('/api/users', data);
        showToast('User created');
        location.reload();
    } catch (e) { showToast(e.message, 'danger'); }
}

async function lockUser(userId) {
    try {
        await API.post('/api/users/lock', { userId: Number(userId) });
        showToast('User locked');
        location.reload();
    } catch (e) { showToast(e.message, 'danger'); }
}

async function unlockUser(userId) {
    try {
        await API.post('/api/users/unlock', { userId: Number(userId) });
        showToast('User unlocked');
        location.reload();
    } catch (e) { showToast(e.message, 'danger'); }
}

function openResetPasswordModal(userId) {
    document.getElementById('resetUserId').value = userId;
    document.getElementById('newPassword').value = '';
    new bootstrap.Modal(document.getElementById('resetPasswordModal')).show();
}

async function submitResetPassword() {
    const userId = document.getElementById('resetUserId').value;
    const password = document.getElementById('newPassword').value;
    if (!password) {
        showToast('Password is required', 'danger');
        return;
    }
    try {
        await API.post('/api/users/reset-password', { userId: Number(userId), password });
        showToast('Password reset');
        bootstrap.Modal.getInstance(document.getElementById('resetPasswordModal')).hide();
    } catch (e) { showToast(e.message, 'danger'); }
}

async function deleteUser(userId) {
    if (!confirm('Delete this user?')) return;
    try {
        await API.post('/api/users/delete', { userId: Number(userId) });
        showToast('User deleted');
        location.reload();
    } catch (e) { showToast(e.message, 'danger'); }
}

document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.lock-user-btn').forEach(btn => {
        btn.addEventListener('click', () => lockUser(btn.dataset.userId));
    });
    document.querySelectorAll('.unlock-user-btn').forEach(btn => {
        btn.addEventListener('click', () => unlockUser(btn.dataset.userId));
    });
    document.querySelectorAll('.reset-password-btn').forEach(btn => {
        btn.addEventListener('click', () => openResetPasswordModal(btn.dataset.userId));
    });
    document.querySelectorAll('.delete-user-btn').forEach(btn => {
        btn.addEventListener('click', () => deleteUser(btn.dataset.userId));
    });
});
