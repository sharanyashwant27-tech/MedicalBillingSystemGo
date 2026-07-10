let orderItems = [];

function openOrderModal() {
    document.getElementById('orderId').value = '';
    document.getElementById('orderModalLabel').textContent = 'New Online Order';
    document.getElementById('orderCustomerId').value = '';
    document.getElementById('orderBranchId').value = '';
    document.getElementById('orderStatus').value = 'PENDING';
    document.getElementById('orderContactPhone').value = '';
    document.getElementById('orderDeliveryAddress').value = '';
    document.getElementById('orderNotes').value = '';
    document.getElementById('orderMedicineId').value = '';
    document.getElementById('orderQuantity').value = '1';
    orderItems = [];
    renderOrderItems();

    const modalEl = document.getElementById('orderModal');
    if (modalEl) {
        bootstrap.Modal.getOrCreateInstance(modalEl).show();
    }
}

function addOrderItem() {
    const medicineSelect = document.getElementById('orderMedicineId');
    const medicineId = medicineSelect.value;
    const quantity = parseInt(document.getElementById('orderQuantity').value, 10);

    if (!medicineId) {
        showToast('Select a medicine', 'warning');
        return;
    }
    if (!quantity || quantity < 1) {
        showToast('Enter a valid quantity', 'warning');
        return;
    }

    const medicineName = medicineSelect.options[medicineSelect.selectedIndex].text;
    orderItems.push({
        medicineId: parseInt(medicineId, 10),
        quantity,
        medicineName
    });

    medicineSelect.value = '';
    document.getElementById('orderQuantity').value = '1';
    renderOrderItems();
}

function removeOrderItem(index) {
    orderItems.splice(index, 1);
    renderOrderItems();
}

function renderOrderItems() {
    const tbody = document.getElementById('orderItemsBody');
    if (!tbody) {
        return;
    }

    if (!orderItems.length) {
        tbody.innerHTML = '<tr><td colspan="3" class="text-muted">No medicines added yet</td></tr>';
        return;
    }

    tbody.innerHTML = orderItems.map((item, index) => `
        <tr>
            <td>${item.medicineName}</td>
            <td>${item.quantity}</td>
            <td>
                <button type="button" class="btn btn-sm btn-outline-danger" onclick="removeOrderItem(${index})">
                    <i class="bi bi-trash"></i>
                </button>
            </td>
        </tr>
    `).join('');
}

function buildOrderPayload() {
    const customerId = document.getElementById('orderCustomerId').value;
    if (!customerId) {
        showToast('Please select a customer', 'warning');
        return null;
    }
    if (!orderItems.length) {
        showToast('Add at least one medicine', 'warning');
        return null;
    }

    const data = {
        customerId: parseInt(customerId, 10),
        status: document.getElementById('orderStatus').value,
        contactPhone: document.getElementById('orderContactPhone').value,
        deliveryAddress: document.getElementById('orderDeliveryAddress').value,
        notes: document.getElementById('orderNotes').value,
        items: orderItems.map(({ medicineId, quantity }) => ({ medicineId, quantity }))
    };

    const branchId = document.getElementById('orderBranchId').value;
    if (branchId) {
        data.branchId = parseInt(branchId, 10);
    } else {
        data.branchId = null;
    }

    return data;
}

async function saveOrder() {
    const data = buildOrderPayload();
    if (!data) {
        return;
    }

    const orderId = document.getElementById('orderId').value;

    try {
        if (orderId) {
            await API.put('/api/online-orders/' + orderId, data);
            showToast('Order updated successfully');
        } else {
            await API.post('/api/online-orders', data);
            showToast('Order created successfully');
        }

        const modalEl = document.getElementById('orderModal');
        const modal = bootstrap.Modal.getInstance(modalEl);
        if (modal) {
            modal.hide();
        }
        location.reload();
    } catch (e) {
        showToast(e.message, 'danger');
    }
}

async function viewOrder(id) {
    try {
        const order = await API.get('/api/online-orders/' + id);
        document.getElementById('viewOrderNumber').textContent = order.orderNumber || '-';
        document.getElementById('viewOrderStatus').textContent = order.status || '-';
        document.getElementById('viewOrderCustomer').textContent = order.customer?.customerName || '-';
        document.getElementById('viewOrderBranch').textContent = order.branch?.branchName || '-';
        document.getElementById('viewOrderPhone').textContent = order.contactPhone || '-';
        document.getElementById('viewOrderAddress').textContent = order.deliveryAddress || '-';
        document.getElementById('viewOrderNotes').textContent = order.notes || '-';
        document.getElementById('viewOrderDate').textContent = order.orderDate
            ? new Date(order.orderDate).toLocaleString()
            : '-';
        document.getElementById('viewOrderTotal').textContent = formatCurrency(order.totalAmount);

        const itemsBody = document.getElementById('viewOrderItemsBody');
        if (!order.items || !order.items.length) {
            itemsBody.innerHTML = '<tr><td colspan="4" class="text-muted">No items</td></tr>';
        } else {
            itemsBody.innerHTML = order.items.map(item => `
                <tr>
                    <td>${item.medicine?.medicineName || '-'}</td>
                    <td>${item.quantity}</td>
                    <td>${formatCurrency(item.unitPrice)}</td>
                    <td>${formatCurrency(item.subtotal)}</td>
                </tr>
            `).join('');
        }

        new bootstrap.Modal(document.getElementById('viewOrderModal')).show();
    } catch (e) {
        showToast(e.message, 'danger');
    }
}

async function editOrder(id) {
    try {
        const order = await API.get('/api/online-orders/' + id);
        document.getElementById('orderId').value = order.id;
        document.getElementById('orderModalLabel').textContent = 'Edit Online Order';
        document.getElementById('orderCustomerId').value = order.customer?.id || '';
        document.getElementById('orderBranchId').value = order.branch?.id || '';
        document.getElementById('orderStatus').value = order.status || 'PENDING';
        document.getElementById('orderContactPhone').value = order.contactPhone || '';
        document.getElementById('orderDeliveryAddress').value = order.deliveryAddress || '';
        document.getElementById('orderNotes').value = order.notes || '';

        orderItems = (order.items || []).map(item => ({
            medicineId: item.medicine.id,
            quantity: item.quantity,
            medicineName: item.medicine.medicineName
        }));
        renderOrderItems();

        new bootstrap.Modal(document.getElementById('orderModal')).show();
    } catch (e) {
        showToast(e.message, 'danger');
    }
}

async function deleteOrder(id) {
    if (!confirm('Delete this online order?')) {
        return;
    }

    try {
        await API.delete('/api/online-orders/' + id);
        showToast('Order deleted successfully');
        location.reload();
    } catch (e) {
        showToast(e.message, 'danger');
    }
}

async function updateStatus(orderId, status) {
    try {
        await API.put('/api/online-orders/' + orderId + '/status', { status });
        showToast('Order status updated to ' + status);
        location.reload();
    } catch (e) {
        showToast(e.message, 'danger');
    }
}
