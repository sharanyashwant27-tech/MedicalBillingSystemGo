let cart = [];
let paymentMode = 'CASH';
let lastSaleId = null;
let qrScanner = null;
let recognition = null;

document.addEventListener('DOMContentLoaded', function() {
    const searchInput = document.getElementById('medicineSearch');
    if (searchInput) {
        let debounce;
        searchInput.addEventListener('input', function() {
            clearTimeout(debounce);
            debounce = setTimeout(() => searchMedicines(this.value), 300);
        });
    }

    document.querySelectorAll('.payment-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            document.querySelectorAll('.payment-btn').forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            paymentMode = this.dataset.mode;
        });
    });

    document.getElementById('amountPaid')?.addEventListener('input', updateReturnMoney);
    document.getElementById('generateBillBtn')?.addEventListener('click', generateBill);
    document.getElementById('printReceiptBtn')?.addEventListener('click', printReceipt);
    document.getElementById('whatsappBtn')?.addEventListener('click', shareWhatsApp);
    document.getElementById('barcodeScanBtn')?.addEventListener('click', toggleQrScanner);
    document.getElementById('voiceBillingBtn')?.addEventListener('click', startVoiceBilling);
});

function toggleQrScanner() {
    const readerDiv = document.getElementById('qrReader');
    if (readerDiv.style.display === 'none') {
        readerDiv.style.display = 'block';
        if (!qrScanner) {
            qrScanner = new Html5Qrcode('qrReader');
            qrScanner.start({ facingMode: 'environment' }, { fps: 10, qrbox: 250 },
                (decodedText) => {
                    searchMedicines(decodedText);
                    stopQrScanner();
                },
                () => {}
            ).catch(() => {
                const code = prompt('Enter barcode or QR code:');
                if (code) searchMedicines(code);
                readerDiv.style.display = 'none';
            });
        }
    } else {
        stopQrScanner();
    }
}

function stopQrScanner() {
    const readerDiv = document.getElementById('qrReader');
    if (qrScanner) {
        qrScanner.stop().then(() => { readerDiv.style.display = 'none'; }).catch(() => { readerDiv.style.display = 'none'; });
    } else {
        readerDiv.style.display = 'none';
    }
}

function startVoiceBilling() {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) { showToast('Voice recognition not supported in this browser', 'warning'); return; }

    const statusEl = document.getElementById('voiceStatus');
    statusEl.style.display = 'block';
    statusEl.textContent = 'Listening... Say medicine name or barcode';

    recognition = new SpeechRecognition();
    recognition.lang = 'en-IN';
    recognition.interimResults = false;
    recognition.onresult = (event) => {
        const transcript = event.results[0][0].transcript.trim();
        statusEl.textContent = 'Heard: ' + transcript;
        document.getElementById('medicineSearch').value = transcript;
        searchMedicines(transcript);
    };
    recognition.onerror = () => { statusEl.textContent = 'Voice recognition failed'; };
    recognition.onend = () => { setTimeout(() => { statusEl.style.display = 'none'; }, 3000); };
    recognition.start();
}

async function shareWhatsApp() {
    if (!lastSaleId) return;
    const phone = prompt('Enter customer WhatsApp number:');
    if (!phone) return;
    try {
        const result = await API.post('/api/sales/' + lastSaleId + '/whatsapp', { phone });
        window.open(result.shareUrl, '_blank');
        showToast('WhatsApp share link opened');
    } catch (e) { showToast(e.message, 'danger'); }
}

async function searchMedicines(query) {
    if (!query || query.length < 1) {
        document.getElementById('searchResults').innerHTML = '';
        return;
    }
    try {
        const results = await API.get('/api/medicines/search?q=' + encodeURIComponent(query));
        const container = document.getElementById('searchResults');
        container.innerHTML = results.map(m => `
            <div class="search-result-item" onclick='addToCart(${JSON.stringify(m).replace(/'/g, "&#39;")})'>
                <strong>${m.medicineName}</strong> - ${m.medicineCode}
                <br><small>Stock: ${m.currentStock} | Price: ₹${m.sellingPrice} | Batch: ${m.batchNumber || 'N/A'}</small>
            </div>
        `).join('');
    } catch (e) {
        showToast(e.message, 'danger');
    }
}

function addToCart(medicine) {
    const existing = cart.find(c => c.medicineId === medicine.id);
    if (existing) {
        if (existing.quantity < medicine.currentStock) existing.quantity++;
        else { showToast('Insufficient stock', 'warning'); return; }
    } else {
        if (medicine.currentStock < 1) { showToast('Out of stock', 'warning'); return; }
        cart.push({
            medicineId: medicine.id,
            medicineName: medicine.medicineName,
            batchNumber: medicine.batchNumber,
            quantity: 1,
            unitPrice: parseFloat(medicine.sellingPrice),
            discountPercent: parseFloat(medicine.discountPercent) || 0,
            gstPercent: parseFloat(medicine.gstPercent) || 0,
            maxStock: medicine.currentStock
        });
    }
    renderCart();
}

function renderCart() {
    const tbody = document.getElementById('cartBody');
    tbody.innerHTML = cart.map((item, i) => {
        const lineTotal = item.unitPrice * item.quantity;
        const discount = lineTotal * item.discountPercent / 100;
        const taxable = lineTotal - discount;
        const gst = taxable * item.gstPercent / 100;
        const subtotal = taxable + gst;
        return `<tr>
            <td>${item.medicineName}</td>
            <td>${item.batchNumber || '-'}</td>
            <td><input type="number" value="${item.quantity}" min="1" max="${item.maxStock}" style="width:60px" onchange="updateQty(${i}, this.value)"></td>
            <td>₹${item.unitPrice.toFixed(2)}</td>
            <td>${item.discountPercent}%</td>
            <td>₹${gst.toFixed(2)}</td>
            <td>₹${subtotal.toFixed(2)}</td>
            <td><button class="btn btn-sm btn-outline-danger" onclick="removeFromCart(${i})"><i class="bi bi-trash"></i></button></td>
        </tr>`;
    }).join('');
    updateSummary();
}

function updateQty(index, qty) {
    qty = parseInt(qty);
    if (qty > 0 && qty <= cart[index].maxStock) cart[index].quantity = qty;
    else showToast('Invalid quantity', 'warning');
    renderCart();
}

function removeFromCart(index) {
    cart.splice(index, 1);
    renderCart();
}

function updateSummary() {
    let subtotal = 0, discount = 0, gst = 0;
    cart.forEach(item => {
        const lineTotal = item.unitPrice * item.quantity;
        const lineDiscount = lineTotal * item.discountPercent / 100;
        const taxable = lineTotal - lineDiscount;
        subtotal += lineTotal;
        discount += lineDiscount;
        gst += taxable * item.gstPercent / 100;
    });
    const grandTotal = subtotal - discount + gst;
    document.getElementById('subtotal').textContent = formatCurrency(subtotal);
    document.getElementById('discount').textContent = formatCurrency(discount);
    document.getElementById('gst').textContent = formatCurrency(gst);
    document.getElementById('grandTotal').textContent = formatCurrency(grandTotal);
    updateReturnMoney();
}

function updateReturnMoney() {
    const grandTotal = parseFloat(document.getElementById('grandTotal').textContent.replace('₹', '')) || 0;
    const paid = parseFloat(document.getElementById('amountPaid').value) || 0;
    document.getElementById('returnMoney').textContent = formatCurrency(Math.max(0, paid - grandTotal));
}

async function generateBill() {
    if (cart.length === 0) { showToast('Cart is empty', 'warning'); return; }
    const grandTotal = parseFloat(document.getElementById('grandTotal').textContent.replace('₹', ''));
    const amountPaid = parseFloat(document.getElementById('amountPaid').value) || grandTotal;
    const customerId = document.getElementById('customerSelect').value;

    const request = {
        customerId: customerId ? parseInt(customerId) : null,
        paymentMode: paymentMode,
        amountPaid: amountPaid,
        items: cart.map(c => ({
            medicineId: c.medicineId,
            quantity: c.quantity,
            discountPercent: c.discountPercent,
            batchNumber: c.batchNumber
        }))
    };

    try {
        const sale = await API.post('/api/sales', request);
        lastSaleId = sale.id;
        showToast('Bill generated: ' + sale.billNumber);
        document.getElementById('printReceiptBtn').disabled = false;
        document.getElementById('whatsappBtn').disabled = false;
        cart = [];
        renderCart();
    } catch (e) {
        showToast(e.message, 'danger');
    }
}

function printReceipt() {
    if (lastSaleId) window.open('/api/sales/' + lastSaleId + '/pdf', '_blank');
}
