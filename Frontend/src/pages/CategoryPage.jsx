import { useState, useEffect } from 'react';
import PlayerTable from '../components/PlayerTable';
import { formatNation, getCodeFromGroup } from '../utils';
import { Search } from 'lucide-react';

function CategoryPage({ type }) {
    const [players, setPlayers] = useState([]);
    const [categories, setCategories] = useState([]);
    const [selectedCategory, setSelectedCategory] = useState(null);

    const [filterTerm, setFilterTerm] = useState('');

    useEffect(() => {
        setSelectedCategory(null);
        setFilterTerm(''); // Reset filter term on category change

        fetch(`${import.meta.env.VITE_API_URL}/api/v1/players/data`)
            .then(res => res.json())
            .then(data => {
                setPlayers(data);
                let uniqueValues = [];

                if (type === 'position') {
                    uniqueValues = ['Guarda-Redes', 'Defesa', 'Médio', 'Avançado'];
                } else if (type === 'nation') {
                    const validNations = data
                        .map(p => formatNation(p.nation))
                        .filter(n => n && n !== 'Unknown' && n.trim() !== '');
                    uniqueValues = [...new Set(validNations)].sort();
                } else {
                    uniqueValues = [...new Set(data.map(p => p[type]))].sort();
                }
                setCategories(uniqueValues);
            });
    }, [type]);

    const visibleCategories = categories.filter(cat =>
        cat.toLowerCase().includes(filterTerm.toLowerCase())
    );

    const filteredPlayers = selectedCategory
        ? players.filter(p => {
            if (type === 'position') return p.position && p.position.includes(getCodeFromGroup(selectedCategory));
            if (type === 'nation') return formatNation(p.nation) === selectedCategory;
            return p[type] === selectedCategory;
        })
        : [];

    return (
        <div>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', marginBottom: '2rem' }}>
                <h2 style={{ fontWeight: 300, marginBottom: '1rem' }}>
                    {type === 'team' ? 'Equipas' : type === 'nation' ? 'Países' : 'Posições'}
                </h2>

                {categories.length > 10 && (
                    <div className="search-box" style={{ width: '100%', maxWidth: '400px', marginBottom: '0' }}>
                        <Search size={18} color="#94a3b8" />
                        <input
                            type="text"
                            placeholder={`Filtrar ${type === 'nation' ? 'países' : 'equipas'}...`}
                            value={filterTerm}
                            onChange={(e) => setFilterTerm(e.target.value)}
                            style={{ padding: '0.5rem' }}
                        />
                    </div>
                )}
            </div>

            {visibleCategories.length === 0 && categories.length > 0 && (
                <p style={{ textAlign: 'center', color: '#94a3b8' }}>Nenhum resultado encontrado.</p>
            )}

            <div className="category-grid">
                {visibleCategories.map(cat => (
                    <button
                        key={cat}
                        className={`cat-btn ${selectedCategory === cat ? 'active' : ''}`}
                        onClick={() => setSelectedCategory(cat)}
                    >
                        {cat}
                    </button>
                ))}
            </div>

            {selectedCategory && <PlayerTable players={filteredPlayers} />}
        </div>
    );
}

export default CategoryPage;