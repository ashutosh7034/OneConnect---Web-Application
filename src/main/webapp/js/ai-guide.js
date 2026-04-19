function getApiBase() {
    const pathParts = window.location.pathname.split('/').filter(Boolean);
    const contextPath = pathParts.length > 1 ? `/${pathParts[0]}` : '';
    return `${window.location.origin}${contextPath}/api`;
}

const API_BASE_AI = getApiBase();
const PYTHON_RAG_URL = 'http://localhost:8000/recommend';

const LOCAL_SERVICES = [
    { name: 'Zomato', url: 'https://www.zomato.com', description: 'Order food online', emoji: '🍔', keywords: ['food', 'hungry', 'eat', 'restaurant', 'delivery', 'lunch', 'dinner'] },
    { name: 'Swiggy', url: 'https://www.swiggy.com', description: 'Food delivery', emoji: '🍲', keywords: ['food', 'hungry', 'eat', 'delivery', 'snacks', 'dinner', 'lunch'] },
    { name: 'Dominos', url: 'https://www.dominos.co.in', description: 'Pizza delivery', emoji: '🍕', keywords: ['food', 'pizza', 'hungry', 'eat', 'delivery'] },
    { name: 'Amazon', url: 'https://www.amazon.in', description: 'Online shopping', emoji: '🛒', keywords: ['shopping', 'buy', 'products', 'electronics', 'order'] },
    { name: 'Flipkart', url: 'https://www.flipkart.com', description: 'Electronics and more', emoji: '🛍️', keywords: ['shopping', 'buy', 'electronics', 'fashion'] },
    { name: 'Myntra', url: 'https://www.myntra.com', description: 'Fashion and clothing', emoji: '👗', keywords: ['shopping', 'fashion', 'clothes', 'style', 'outfit'] },
    { name: 'Meesho', url: 'https://www.meesho.com', description: 'Resell and shop', emoji: '📦', keywords: ['shopping', 'resell', 'store', 'products', 'budget'] },
    { name: 'YouTube', url: 'https://www.youtube.com', description: 'Watch videos', emoji: '📺', keywords: ['videos', 'learn', 'music', 'entertainment', 'tutorial'] },
    { name: 'Netflix', url: 'https://www.netflix.com', description: 'Movies and TV shows', emoji: '🎬', keywords: ['movies', 'series', 'shows', 'entertainment', 'watch'] },
    { name: 'Spotify', url: 'https://open.spotify.com', description: 'Music streaming', emoji: '🎵', keywords: ['music', 'songs', 'podcast', 'audio', 'playlist'] },
    { name: 'Google Maps', url: 'https://maps.google.com', description: 'Navigation and transit', emoji: '🗺️', keywords: ['maps', 'navigation', 'route', 'location', 'travel'] },
    { name: 'IRCTC', url: 'https://www.irctc.co.in', description: 'Train ticket booking', emoji: '🚆', keywords: ['train', 'travel', 'ticket', 'journey', 'booking'] },
    { name: 'RedBus', url: 'https://www.redbus.in', description: 'Bus ticket booking', emoji: '🚌', keywords: ['bus', 'travel', 'ticket', 'journey', 'booking'] },
    { name: 'MakeMyTrip', url: 'https://www.makemytrip.com', description: 'Flight, hotel and travel booking', emoji: '✈️', keywords: ['flight', 'aeroplane', 'airplane', 'travel', 'booking', 'trip'] },
    { name: 'Ixigo', url: 'https://www.ixigo.com', description: 'Train and flight booking', emoji: '🎟️', keywords: ['train', 'flight', 'travel', 'ticket', 'booking'] },
    { name: 'Uber', url: 'https://www.uber.com/in/en', description: 'Book rides and cabs', emoji: '🚕', keywords: ['cab', 'ride', 'travel', 'taxi', 'commute'] },
    { name: 'Ola', url: 'https://www.olacabs.com', description: 'Cab booking', emoji: '🚖', keywords: ['cab', 'ride', 'travel', 'taxi', 'commute'] },
    { name: 'Rapido', url: 'https://www.rapido.bike', description: 'Bike taxi for short city rides', emoji: '🏍️', keywords: ['bike', 'taxi', 'ride', 'travel', 'short distance', 'local'] },
    { name: 'Paytm', url: 'https://paytm.com', description: 'Payments and recharges', emoji: '💳', keywords: ['payment', 'bill', 'recharge', 'wallet', 'money'] },
    { name: 'PhonePe', url: 'https://www.phonepe.com', description: 'UPI and payments', emoji: '📱', keywords: ['upi', 'payment', 'money', 'transfer', 'recharge'] },
    { name: 'Google Pay', url: 'https://pay.google.com', description: 'UPI payments and money transfer', emoji: '💰', keywords: ['google pay', 'gpay', 'upi', 'payment', 'money', 'transfer', 'bill'] }
];

const SYNONYMS = {
    hungry: ['food', 'eat', 'delivery', 'restaurant', 'lunch', 'dinner'],
    food: ['hungry', 'eat', 'delivery', 'restaurant'],
    shopping: ['buy', 'fashion', 'electronics', 'products'],
    buy: ['shopping', 'products', 'order'],
    movie: ['movies', 'series', 'shows', 'watch', 'entertainment'],
    music: ['songs', 'playlist', 'podcast', 'audio'],
    travel: ['cab', 'taxi', 'ride', 'maps', 'route'],
    payment: ['pay', 'upi', 'bill', 'recharge', 'money'],
    pay: ['payment', 'upi', 'bill', 'money']
};

const chat = document.getElementById('aiChat');
const form = document.getElementById('aiGuideForm');
const queryInput = document.getElementById('aiQuery');
const askBtn = document.getElementById('askAiBtn');
const suggestionsGrid = document.getElementById('aiSuggestions');

let pendingTravelChoice = false;
let lastAiReplyText = '';
const recentAiReplies = [];
const MAX_RECENT_AI_REPLIES = 6;

const SHORT_DISTANCE_THRESHOLD_KM = 30;
const LONG_DISTANCE_THRESHOLD_KM = 80;

const SMALL_TALK_RESPONSES = {
    greeting: [
        'Hello! How can I help you today? You can ask things like "I am hungry", "Need a cab", or "I want shopping apps".',
        'Hi there! Tell me what you need and I will suggest the best apps for it.',
        'Hey! Share your need in simple words and I will recommend the right apps.'
    ],
    wellbeing: [
        'I am doing great and ready to help. Tell me what you need and I will suggest the best apps.',
        'Doing well. I am ready to recommend apps for food, travel, shopping, payments, and more.',
        'All good here. What do you want to do right now?'
    ],
    help: [
        'I can recommend apps for your needs. Try queries like: "I am hungry", "Need payment app", "Suggest travel apps", or "I want movie apps".',
        'Ask me naturally, like "need a cab", "want to watch movies", or "pay bill" and I will suggest apps.',
        'Tell me your goal and I will suggest matching apps with quick links to open them.'
    ],
    thanks: [
        'You are welcome! If you need anything else, just ask.',
        'Happy to help. Ask anytime for more app suggestions.',
        'Glad I could help. Tell me your next need whenever you want.'
    ],
    bye: [
        'Goodbye! Come back anytime, I am here to help you find the right apps.',
        'See you soon. I will be here whenever you need app suggestions.',
        'Bye for now. Have a great day.'
    ]
};

function pickNonRepeatingReply(options) {
    if (!Array.isArray(options) || options.length === 0) {
        return 'Tell me your need and I will suggest matching apps.';
    }

    const normalizedLast = (lastAiReplyText || '').trim();
    const candidates = options.filter((msg) => {
        const normalized = msg.trim();
        return normalized !== normalizedLast && !recentAiReplies.includes(normalized);
    });
    const pool = candidates.length ? candidates : options;
    return pool[Math.floor(Math.random() * pool.length)];
}

function formatList(items) {
    if (!items || items.length === 0) return '';
    if (items.length === 1) return items[0];
    if (items.length === 2) return `${items[0]} and ${items[1]}`;
    return `${items.slice(0, -1).join(', ')}, and ${items[items.length - 1]}`;
}

function buildDynamicIntentReply(intent, serviceNames, extras = {}) {
    const names = formatList(serviceNames);

    const templates = {
        food: [
            `You can try ${names} for food right now.`,
            `For eating options, ${names} are great picks.`,
            `Best food apps for this are ${names}.`
        ],
        shopping: [
            `For shopping, I recommend ${names}.`,
            `${names} should match your shopping need well.`,
            `Try ${names} for this shopping request.`
        ],
        payments: [
            `For payments, use ${names}.`,
            `${names} are reliable choices for UPI and bills.`,
            `I suggest ${names} for payment and recharge tasks.`
        ],
        entertainment: [
            `For entertainment, go with ${names}.`,
            `${names} are good options for shows, videos, and music.`,
            `You can start with ${names} for entertainment.`
        ],
        travel_long: [
            `For long-distance travel, use ${names}.`,
            `Long trip detected, so ${names} are the best fit.`,
            `For outstation travel, I recommend ${names}.`
        ],
        travel_short: [
            `For short-distance travel, choose ${names}.`,
            `Local ride need: ${names} should work well.`,
            `For nearby travel, use ${names}.`
        ],
        travel_medium: [
            `For medium-distance travel, try ${names}.`,
            `${names} are practical for this distance range.`,
            `For this trip, ${names} are balanced options.`
        ],
        travel_followup: [
            'Should I suggest long-distance or short-distance travel apps?',
            'Tell me travel type: long-distance or short-distance?',
            'Is this a local trip or outstation trip?'
        ],
        local_mode: [
            'I am in local recommendation mode, but I can still suggest strong matches.',
            'Using local smart matching right now. Here are the best options.',
            'Backend AI is not available, so I switched to local smart suggestions.'
        ]
    };

    const key = extras.variant ? `${intent}_${extras.variant}` : intent;
    const options = templates[key] || templates[intent] || ['I found a few good options for you.'];
    return pickNonRepeatingReply(options);
}

const INTENT_KEYWORDS = {
    food: ['hungry', 'food', 'eat', 'lunch', 'dinner', 'snack', 'restaurant', 'pizza'],
    shopping: ['shop', 'shopping', 'buy', 'fashion', 'clothes', 'electronics', 'order'],
    entertainment: ['movie', 'movies', 'series', 'show', 'watch', 'entertainment', 'music', 'song', 'podcast', 'video'],
    payments: ['pay', 'payment', 'upi', 'recharge', 'bill', 'money', 'transfer', 'wallet']
};

function normalizeToken(token) {
    return token.toLowerCase().replace(/[^a-z0-9]/g, '');
}

function splitNormalizedTokens(text) {
    return text
        .toLowerCase()
        .split(/\s+/)
        .map(normalizeToken)
        .filter(Boolean);
}

function levenshtein(a, b) {
    if (a === b) return 0;
    if (!a.length) return b.length;
    if (!b.length) return a.length;

    const dp = Array.from({ length: a.length + 1 }, () => new Array(b.length + 1).fill(0));

    for (let i = 0; i <= a.length; i += 1) dp[i][0] = i;
    for (let j = 0; j <= b.length; j += 1) dp[0][j] = j;

    for (let i = 1; i <= a.length; i += 1) {
        for (let j = 1; j <= b.length; j += 1) {
            const cost = a[i - 1] === b[j - 1] ? 0 : 1;
            dp[i][j] = Math.min(
                dp[i - 1][j] + 1,
                dp[i][j - 1] + 1,
                dp[i - 1][j - 1] + cost
            );
        }
    }

    return dp[a.length][b.length];
}

function isFuzzyMatch(queryToken, candidateToken) {
    if (!queryToken || !candidateToken) return false;
    if (queryToken === candidateToken) return true;
    if (candidateToken.startsWith(queryToken) || queryToken.startsWith(candidateToken)) return true;

    const maxLen = Math.max(queryToken.length, candidateToken.length);
    const allowed = maxLen <= 4 ? 1 : maxLen <= 8 ? 2 : 3;
    return levenshtein(queryToken, candidateToken) <= allowed;
}

function queryHasIntentKeyword(text, keywords) {
    const queryTokens = splitNormalizedTokens(text);
    return queryTokens.some((qt) => keywords.some((kw) => isFuzzyMatch(qt, normalizeToken(kw))));
}

function detectTravelIntent(text) {
    return /\b(travel|trip|journey|commute|go somewhere|ride|cab|taxi|flight|train|bus)\b/.test(text);
}

function isLongDistance(text) {
    return /\b(long|long-distance|long distance|outstation|intercity|flight|aeroplane|airplane|air|train|bus|far)\b/.test(text);
}

function isShortDistance(text) {
    return /\b(short|short-distance|short distance|local|nearby|within city|in city|city ride|bike|auto|cab|taxi|quick)\b/.test(text);
}

function parseDistanceKm(text) {
    const kmMatch = text.match(/(\d+(?:\.\d+)?)\s*(km|kilometer|kilometre|kilometers|kilometres)\b/);
    if (kmMatch) {
        return parseFloat(kmMatch[1]);
    }

    const mMatch = text.match(/(\d+(?:\.\d+)?)\s*(m|meter|metre|meters|metres)\b/);
    if (mMatch) {
        return parseFloat(mMatch[1]) / 1000;
    }

    return null;
}

function resolveTravelType(text) {
    if (isLongDistance(text)) {
        return { type: 'long', reason: 'keyword' };
    }

    if (isShortDistance(text)) {
        return { type: 'short', reason: 'keyword' };
    }

    const distanceKm = parseDistanceKm(text);
    if (distanceKm !== null) {
        if (distanceKm <= SHORT_DISTANCE_THRESHOLD_KM) {
            return { type: 'short', reason: 'distance', distanceKm };
        }

        if (distanceKm >= LONG_DISTANCE_THRESHOLD_KM) {
            return { type: 'long', reason: 'distance', distanceKm };
        }

        return { type: 'medium', reason: 'distance', distanceKm };
    }

    return { type: 'unknown' };
}

function inferIntent(text) {
    if (detectTravelIntent(text)) return 'travel';
    if (queryHasIntentKeyword(text, INTENT_KEYWORDS.food)) return 'food';
    if (queryHasIntentKeyword(text, INTENT_KEYWORDS.shopping)) return 'shopping';
    if (queryHasIntentKeyword(text, INTENT_KEYWORDS.entertainment)) return 'entertainment';
    if (queryHasIntentKeyword(text, INTENT_KEYWORDS.payments)) return 'payments';
    return 'generic';
}

function pickServicesByNames(names) {
    return LOCAL_SERVICES.filter((s) => names.includes(s.name)).map((s) => ({ ...s, reason: 'Recommended by your travel preference' }));
}

function classifySmallTalk(query) {
    const text = query.toLowerCase().trim();

    if (/^(hi|hello|hey|hii|hola|yo)\b/.test(text)) {
        return {
            handled: true,
            reply: pickNonRepeatingReply(SMALL_TALK_RESPONSES.greeting)
        };
    }

    if (/\b(how are you|how r u|how are u)\b/.test(text)) {
        return {
            handled: true,
            reply: pickNonRepeatingReply(SMALL_TALK_RESPONSES.wellbeing)
        };
    }

    if (/\b(help|what can you do|how to use)\b/.test(text)) {
        return {
            handled: true,
            reply: pickNonRepeatingReply(SMALL_TALK_RESPONSES.help)
        };
    }

    if (/\b(thank you|thanks|thx)\b/.test(text)) {
        return {
            handled: true,
            reply: pickNonRepeatingReply(SMALL_TALK_RESPONSES.thanks)
        };
    }

    if (/\b(bye|goodbye|see you)\b/.test(text)) {
        return {
            handled: true,
            reply: pickNonRepeatingReply(SMALL_TALK_RESPONSES.bye)
        };
    }

    return { handled: false };
}

function addMessage(text, role) {
    const row = document.createElement('div');
    row.className = `chat-msg chat-msg-${role}`;

    const bubble = document.createElement('div');
    bubble.className = 'chat-bubble';
    bubble.textContent = text;

    row.appendChild(bubble);
    chat.appendChild(row);
    chat.scrollTop = chat.scrollHeight;

    if (role === 'ai') {
        lastAiReplyText = text;
        recentAiReplies.push(text.trim());
        if (recentAiReplies.length > MAX_RECENT_AI_REPLIES) {
            recentAiReplies.shift();
        }
    }
}

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

function localRecommend(query) {
    const q = query.toLowerCase().trim();
    const tokens = splitNormalizedTokens(q);
    const expanded = new Set(tokens);

    tokens.forEach((t) => {
        if (SYNONYMS[t]) {
            SYNONYMS[t].forEach((x) => expanded.add(x));
        }
    });

    const scored = LOCAL_SERVICES.map((service) => {
        let score = 0;
        const matched = [];

        const name = service.name.toLowerCase();
        const desc = service.description.toLowerCase();
        const serviceTokens = splitNormalizedTokens(`${service.name} ${service.description} ${service.keywords.join(' ')}`);

        if (q.includes(name) || splitNormalizedTokens(name).some((nt) => tokens.some((qt) => isFuzzyMatch(qt, nt)))) {
            score += 6;
            matched.push(service.name);
        }

        expanded.forEach((term) => {
            if (name.includes(term)) score += 3;
            if (desc.includes(term)) score += 1;

            const fuzzyKeywordHit = serviceTokens.some((st) => isFuzzyMatch(term, st));
            if (fuzzyKeywordHit || service.keywords.some((k) => normalizeToken(k).includes(term) || term.includes(normalizeToken(k)))) {
                score += 2;
                matched.push(term);
            }
        });

        return {
            ...service,
            score,
            reason: matched.length ? `Matches: ${Array.from(new Set(matched)).slice(0, 2).join(', ')}` : 'Relevant option'
        };
    }).filter((s) => s.score > 0)
      .sort((a, b) => b.score - a.score)
      .slice(0, 5);

    const suggestions = scored.length ? scored : LOCAL_SERVICES.slice(0, 5).map((s) => ({ ...s, reason: 'Popular option' }));
    const topNames = suggestions.slice(0, 3).map((s) => s.name);

    return {
        success: true,
        reply: pickNonRepeatingReply([
            `Based on your query, try ${topNames.join(', ')}.`,
            `I matched your need with ${topNames.join(', ')}.`,
            `Top recommendations for this are ${topNames.join(', ')}.`
        ]),
        suggestions
    };
}

function renderSuggestions(suggestions) {
    suggestionsGrid.innerHTML = '';

    if (!suggestions || suggestions.length === 0) {
        suggestionsGrid.innerHTML = `
            <div class="empty-state" style="grid-column: 1 / -1;">
                <i class="fa-solid fa-magnifying-glass"></i>
                <h3>No strong app matches</h3>
                <p>Try a more specific request like "I am hungry" or "Need payment app".</p>
            </div>
        `;
        return;
    }

    suggestions.forEach((service) => {
        const card = document.createElement('div');
        card.className = 'service-card';
        card.innerHTML = `
            <div class="card-icon">${buildServiceIcon(service)}</div>
            <div class="card-title">${service.name}</div>
            <div class="card-desc">${service.description}</div>
            <div class="card-desc" style="margin-bottom: 1rem; color: #475569;">${service.reason || 'Recommended by AI Guide'}</div>
            <div class="card-actions">
                <a href="${normalizeUrl(service.url)}" target="oneconnect_external" rel="noopener noreferrer" class="btn-open">Open</a>
            </div>
        `;
        suggestionsGrid.appendChild(card);
    });
}

async function askAi(query) {
    addMessage(query, 'user');

    const lower = query.toLowerCase().trim();

    const smallTalk = classifySmallTalk(query);
    if (smallTalk.handled) {
        addMessage(smallTalk.reply, 'ai');
        return;
    }

    const intent = inferIntent(lower);

    if (pendingTravelChoice) {
        const travelType = resolveTravelType(lower);

        if (travelType.type === 'long') {
            pendingTravelChoice = false;
            const suggestions = pickServicesByNames(['MakeMyTrip', 'Ixigo', 'IRCTC', 'RedBus', 'Google Maps']);
            addMessage(buildDynamicIntentReply('travel', suggestions.map((s) => s.name), { variant: 'long' }), 'ai');
            renderSuggestions(suggestions);
            return;
        }

        if (travelType.type === 'short') {
            pendingTravelChoice = false;
            const suggestions = pickServicesByNames(['Rapido', 'Ola', 'Uber', 'Google Maps']);
            if (travelType.reason === 'distance') {
                addMessage(pickNonRepeatingReply([
                    `Since ${travelType.distanceKm} km is short-distance, I recommend ${formatList(suggestions.map((s) => s.name))}.`,
                    `${travelType.distanceKm} km looks like a local ride, so use ${formatList(suggestions.map((s) => s.name))}.`,
                    `For ${travelType.distanceKm} km, short-trip apps like ${formatList(suggestions.map((s) => s.name))} are ideal.`
                ]), 'ai');
            } else {
                addMessage(buildDynamicIntentReply('travel', suggestions.map((s) => s.name), { variant: 'short' }), 'ai');
            }
            renderSuggestions(suggestions);
            return;
        }

        if (travelType.type === 'medium') {
            pendingTravelChoice = false;
            const suggestions = pickServicesByNames(['Ola', 'Uber', 'IRCTC', 'RedBus', 'Google Maps']);
            addMessage(pickNonRepeatingReply([
                `Around ${travelType.distanceKm} km is medium-distance, so ${formatList(suggestions.map((s) => s.name))} are good options.`,
                `For about ${travelType.distanceKm} km, I suggest ${formatList(suggestions.map((s) => s.name))}.`,
                `${travelType.distanceKm} km can be covered well using ${formatList(suggestions.map((s) => s.name))}.`
            ]), 'ai');
            renderSuggestions(suggestions);
            return;
        }

        addMessage(pickNonRepeatingReply([
            'Please tell me if your travel is long-distance or short-distance so I can suggest better apps. You can also type a distance like 12 km.',
            'I can refine this better if you share travel type: short-distance or long-distance (or distance in km).',
            'Tell me the distance or say short-distance/long-distance and I will suggest exact travel apps.'
        ]), 'ai');
        return;
    }

    if (intent === 'travel') {
        const travelType = resolveTravelType(lower);

        if (travelType.type === 'long') {
            const suggestions = pickServicesByNames(['MakeMyTrip', 'Ixigo', 'IRCTC', 'RedBus', 'Google Maps']);
            addMessage(buildDynamicIntentReply('travel', suggestions.map((s) => s.name), { variant: 'long' }), 'ai');
            renderSuggestions(suggestions);
            return;
        }

        if (travelType.type === 'short') {
            const suggestions = pickServicesByNames(['Rapido', 'Ola', 'Uber', 'Google Maps']);
            if (travelType.reason === 'distance') {
                addMessage(pickNonRepeatingReply([
                    `Since ${travelType.distanceKm} km is short-distance, I recommend ${formatList(suggestions.map((s) => s.name))}.`,
                    `${travelType.distanceKm} km looks like a local ride, so use ${formatList(suggestions.map((s) => s.name))}.`,
                    `For ${travelType.distanceKm} km, short-trip apps like ${formatList(suggestions.map((s) => s.name))} are ideal.`
                ]), 'ai');
            } else {
                addMessage(buildDynamicIntentReply('travel', suggestions.map((s) => s.name), { variant: 'short' }), 'ai');
            }
            renderSuggestions(suggestions);
            return;
        }

        if (travelType.type === 'medium') {
            const suggestions = pickServicesByNames(['Ola', 'Uber', 'IRCTC', 'RedBus', 'Google Maps']);
            addMessage(buildDynamicIntentReply('travel', suggestions.map((s) => s.name), { variant: 'medium' }), 'ai');
            renderSuggestions(suggestions);
            return;
        }

        pendingTravelChoice = true;
        addMessage(buildDynamicIntentReply('travel', [], { variant: 'followup' }), 'ai');
        addMessage(pickNonRepeatingReply([
            'Long-distance: flight/train/bus apps. Short-distance: Rapido/Ola/Uber. You can also type distance like 12 km.',
            'If it is far, I will suggest flight/train/bus apps. If it is local, I will suggest ride apps like Rapido/Ola/Uber.',
            'Share distance (example 12 km) and I will auto-pick short, medium, or long travel apps.'
        ]), 'ai');
        return;
    }

    if (intent === 'food') {
        const suggestions = pickServicesByNames(['Zomato', 'Swiggy', 'Dominos']);
        addMessage(buildDynamicIntentReply('food', suggestions.map((s) => s.name)), 'ai');
        renderSuggestions(suggestions);
        return;
    }

    if (intent === 'payments') {
        const suggestions = pickServicesByNames(['PhonePe', 'Paytm']);
        addMessage(buildDynamicIntentReply('payments', suggestions.map((s) => s.name)), 'ai');
        renderSuggestions(suggestions);
        return;
    }

    if (intent === 'shopping') {
        const suggestions = pickServicesByNames(['Amazon', 'Flipkart', 'Myntra', 'Meesho']);
        addMessage(buildDynamicIntentReply('shopping', suggestions.map((s) => s.name)), 'ai');
        renderSuggestions(suggestions);
        return;
    }

    if (intent === 'entertainment') {
        const suggestions = pickServicesByNames(['YouTube', 'Netflix', 'Spotify']);
        addMessage(buildDynamicIntentReply('entertainment', suggestions.map((s) => s.name)), 'ai');
        renderSuggestions(suggestions);
        return;
    }

    askBtn.disabled = true;
    askBtn.textContent = 'Thinking...';

    try {
        let data;

        try {
            const pyResponse = await fetch(PYTHON_RAG_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ query: query })
            });

            if (!pyResponse.ok) {
                throw new Error('Python RAG endpoint failed');
            }

            data = await pyResponse.json();
        } catch (pyError) {
            const javaResponse = await fetch(`${API_BASE_AI}/ai-guide`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ query: query })
            });

            if (!javaResponse.ok) {
                throw new Error('Java AI endpoint failed');
            }
            data = await javaResponse.json();
        }

        addMessage(data.reply || 'I could not generate suggestions right now.', 'ai');
        renderSuggestions(data.suggestions || []);
    } catch (error) {
        const local = localRecommend(query);
        addMessage(buildDynamicIntentReply('local', [], { variant: 'mode' }), 'ai');
        addMessage(local.reply, 'ai');
        renderSuggestions(local.suggestions || []);
    } finally {
        askBtn.disabled = false;
        askBtn.textContent = 'Ask AI';
    }
}

form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const query = queryInput.value.trim();
    if (!query) {
        return;
    }

    queryInput.value = '';
    await askAi(query);
});

document.querySelectorAll('.quick-chip').forEach((chip) => {
    chip.addEventListener('click', async () => {
        const query = chip.getAttribute('data-query');
        if (query) {
            await askAi(query);
        }
    });
});
