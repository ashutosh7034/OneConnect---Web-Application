function getApiBase() {
    const pathParts = window.location.pathname.split('/').filter(Boolean);
    const contextPath = pathParts.length > 1 ? `/${pathParts[0]}` : '';
    return `${window.location.origin}${contextPath}/api`;
}

const API_BASE_SEARCH = getApiBase();

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
    const urlParams = new URLSearchParams(window.location.search);
    const q = urlParams.get('q');
    
    if (q) {
        document.getElementById('searchInput').value = q;
        document.getElementById('searchQueryText').textContent = `Showing results for "${q}"`;
        performSearch(q);
    } else {
        document.getElementById('searchQueryText').textContent = "Please enter a search term.";
        document.getElementById('searchGrid').innerHTML = '';
    }
});

async function performSearch(query) {
    const grid = document.getElementById('searchGrid');
    try {
        const response = await fetch(`${API_BASE_SEARCH}/search?q=${encodeURIComponent(query)}`);
        const results = await response.json();

        if (results.length === 0) {
            grid.innerHTML = `
                <div class="empty-state" style="grid-column: 1 / -1;">
                    <i class="fa-solid fa-magnifying-glass"></i>
                    <h3>No services found</h3>
                    <p>We couldn't find any service matching "${query}".</p>
                </div>
            `;
            return;
        }

        grid.innerHTML = '';
        results.forEach(service => {
            const card = document.createElement('div');
            card.className = 'service-card';
            card.innerHTML = `
                <div class="card-icon">${buildServiceIcon(service)}</div>
                <div class="card-title">${service.name}</div>
                <div class="card-desc">${service.description}</div>
                <div class="card-actions">
                    ${buildOpenAction(service.name, service.url)}
                </div>
            `;
            grid.appendChild(card);
        });
    } catch (error) {
        console.error("Error searching:", error);
        grid.innerHTML = `
            <div class="empty-state" style="grid-column: 1 / -1;">
                <i class="fa-solid fa-triangle-exclamation"></i>
                <p>Failed to load search results.</p>
            </div>
        `;
    }
}
