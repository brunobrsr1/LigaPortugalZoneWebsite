const nationMap = {

    'HND': 'Honduras', 'HON': 'Honduras', 'HO': 'Honduras',
    'PRI': 'Porto Rico', 'PUR': 'Porto Rico', 'PU': 'Porto Rico', 'PR': 'Porto Rico',
    'CPV': 'Cabo Verde', 'CV': 'Cabo Verde',
    'GNB': 'Guiné-Bissau', 'GW': 'Guiné-Bissau',

    'PRT': 'Portugal', 'POR': 'Portugal',
    'ESP': 'Espanha', 'SPA': 'Espanha',
    'FRA': 'França',
    'DEU': 'Alemanha', 'GER': 'Alemanha',
    'ENG': 'Inglaterra',
    'ITA': 'Itália',
    'NLD': 'Holanda', 'NED': 'Holanda',
    'BEL': 'Bélgica',
    'CHE': 'Suíça', 'SUI': 'Suíça',
    'SWE': 'Suécia',
    'DNK': 'Dinamarca', 'DEN': 'Dinamarca',
    'NOR': 'Noruega',
    'GEO': 'Geórgia',
    'UKR': 'Ucrânia',
    'SRB': 'Sérvia',
    'HRV': 'Croácia', 'CRO': 'Croácia',
    'POL': 'Polónia',
    'CZE': 'Rep. Checa',
    'GRC': 'Grécia', 'GRE': 'Grécia',
    'TUR': 'Turquia',
    'HUN': 'Hungria',
    'SVN': 'Eslovénia',
    'SVK': 'Eslováquia',
    'ROU': 'Roménia', 'ROM': 'Roménia',
    'AUT': 'Áustria',
    'SCO': 'Escócia',
    'IRL': 'Irlanda',
    'RUS': 'Rússia',
    'ALB': 'Albânia',
    'BIH': 'Bósnia',
    'FIN': 'Finlândia',
    'ISL': 'Islândia',
    'MNE': 'Montenegro',
    'MKD': 'Macedónia',
    'BGR': 'Bulgária', 'BUL': 'Bulgária',
    'ISR': 'Israel',

    'BRA': 'Brasil',
    'ARG': 'Argentina',
    'URY': 'Uruguai', 'URU': 'Uruguai',
    'COL': 'Colômbia',
    'CHL': 'Chile',
    'VEN': 'Venezuela',
    'ECU': 'Equador',
    'PRY': 'Paraguai', 'PAR': 'Paraguai',
    'PER': 'Peru',
    'BOL': 'Bolívia',

    'AGO': 'Angola', 'ANG': 'Angola',
    'MOZ': 'Moçambique',
    'STP': 'São Tomé',
    'NGA': 'Nigéria', 'NGR': 'Nigéria',
    'SEN': 'Senegal',
    'MAR': 'Marrocos',
    'DZA': 'Argélia', 'ALG': 'Argélia',
    'EGY': 'Egito',
    'CIV': 'Costa do Marfim',
    'GHA': 'Gana',
    'CMR': 'Camarões',
    'MLI': 'Mali',
    'GIN': 'Guiné',
    'ZAF': 'África do Sul', 'RSA': 'África do Sul',
    'TUN': 'Tunísia',
    'GAB': 'Gabão',
    'COG': 'Congo',

    'JPN': 'Japão',
    'KOR': 'Coreia do Sul',
    'IRN': 'Irão', 'IRI': 'Irão',
    'USA': 'EUA',
    'CAN': 'Canadá',
    'MEX': 'México',
    'PAN': 'Panamá',
    'CRI': 'Costa Rica',
    'AUS': 'Austrália'
};

export const formatNation = (rawNation) => {
    if (!rawNation) return '';
    const clean = rawNation.trim();

    const parts = clean.split(' ');
    let code = parts.length > 1 ? parts[parts.length - 1] : parts[0];

    code = code.toUpperCase().replace(/[^A-Z]/g, '');

    if (nationMap[code]) return nationMap[code];

    try {
        const regionNames = new Intl.DisplayNames(['pt'], { type: 'region' });
        return regionNames.of(code.substring(0, 2));
    } catch (e) {
        return code;
    }
};

export const mapPositionToGroup = (pos) => {
    if (!pos) return 'Outros';
    if (pos.includes('GK')) return 'Guarda-Redes';
    if (pos.includes('DF')) return 'Defesa';
    if (pos.includes('MF')) return 'Médio';
    if (pos.includes('FW')) return 'Avançado';
    return 'Outros';
};

export const getCodeFromGroup = (group) => {
    switch(group) {
        case 'Guarda-Redes': return 'GK';
        case 'Defesa': return 'DF';
        case 'Médio': return 'MF';
        case 'Avançado': return 'FW';
        default: return '';
    }
};