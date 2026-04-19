function getApiBase() {
    const pathParts = window.location.pathname.split('/').filter(Boolean);
    const contextPath = pathParts.length > 1 ? `/${pathParts[0]}` : '';
    return `${window.location.origin}${contextPath}/api`;
}

const API_BASE_DASHBOARD = getApiBase();
let userFavourites = [];

function normalizeUrl(rawUrl) {
    if (!rawUrl) return '';
    return rawUrl.startsWith('http') ? rawUrl : `https://${rawUrl}`;
}

function getLogoUrl(serviceUrl) {
    try {
        const host = new URL(normalizeUrl(serviceUrl)).hostname;
        return `https://www.google.com/s2/favicons?domain=${encodeURIComponent(host)}&sz=128`;
    } catch (error) {
        return '';
    }
}

function buildServiceIcon(service) {
    const logoUrl = getLogoUrl(service.url);
    const fallback = ((service.name || '?').trim().charAt(0) || '?').toUpperCase();

    if (!logoUrl) {
        return `<span class="service-logo-fallback">${fallback}</span>`;
    }

    return `
        <img src="${logoUrl}" alt="${service.name} logo" class="service-logo" loading="lazy" referrerpolicy="no-referrer"
             onerror="this.style.display='none'; this.nextElementSibling.style.display='flex';">
        <span class="service-logo-fallback" style="display:none;">${fallback}</span>
    `;
}

function buildOpenAction(name, url) {
    const normalizedUrl = normalizeUrl(url);
    return `<a href="${normalizedUrl}" target="oneconnect_external" rel="noopener noreferrer" class="btn-open" title="Opens in external tab, OneConnect stays open">Open</a>`;
}

document.addEventListener('DOMContentLoaded', async () => {
    const userId = localStorage.getItem('userId');
    if (!userId) return; // Will be redirected by HTML script

    try {
        // Fetch user favourites first to set heart button state
        const favRes = await fetch(`${API_BASE_DASHBOARD}/favourites?userId=${userId}`);
        userFavourites = await favRes.json();
        
        // Fetch all services
        const res = await fetch(`${API_BASE_DASHBOARD}/services`);
        const services = await res.json();
        
        renderServices(services);
    } catch (error) {
        console.error("Error loading dashboard data:", error);
        document.getElementById('servicesGrid').innerHTML = `
            <div class="empty-state" style="grid-column: 1 / -1;">
                <i class="fa-solid fa-triangle-exclamation"></i>
                <h3>Error Loading Services</h3>
                <p>Please check your connection or backend server.</p>
            </div>
        `;
    }
});

function isFavourite(serviceName) {
    return userFavourites.some(fav => fav.name === serviceName);
}

function renderServices(services) {
    const grid = document.getElementById('servicesGrid');
    grid.innerHTML = '';

    services.forEach(service => {
        const isFav = isFavourite(service.name);
        const card = document.createElement('div');
        card.className = 'service-card';
        card.innerHTML = `
            <div class="card-icon">${buildServiceIcon(service)}</div>
            <div class="card-title">${service.name}</div>
            <div class="card-desc">${service.description}</div>
            <div class="card-actions">
                ${buildOpenAction(service.name, service.url)}
                <button class="btn-fav ${isFav ? 'active' : ''}" onclick="toggleFavourite(this, '${service.name}', '${service.url}')">
                    <i class="fa-${isFav ? 'solid' : 'regular'} fa-heart"></i>
                </button>
            </div>
        `;
        grid.appendChild(card);
    });
}

async function toggleFavourite(btn, serviceName, serviceUrl) {
    const userId = localStorage.getItem('userId');
    const isAdding = !btn.classList.contains('active');
    
    try {
        const url = `${API_BASE_DASHBOARD}/favourites`;
        const method = isAdding ? 'POST' : 'DELETE';
        
        const response = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId, serviceName, serviceUrl })
        });
        
        const data = await response.json();
        if (data.success) {
            btn.classList.toggle('active');
            const icon = btn.querySelector('i');
            if (isAdding) {
                icon.classList.remove('fa-regular');
                icon.classList.add('fa-solid');
            } else {
                icon.classList.remove('fa-solid');
                icon.classList.add('fa-regular');
            }
        }
    } catch (error) {
        console.error("Error toggling favourite:", error);
    }
}
