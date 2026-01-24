import { useState } from 'react';
import PlayerTable from '../components/PlayerTable';
import { Search } from 'lucide-react';

function SearchPage() {
    const [query, setQuery] = useState('');
    const [players, setPlayers] = useState([]);
    const [hasSearched, setHasSearched] = useState(false);

    const handleSearch = () => {
        fetch(`${import.meta.env.VITE_API_URL}/api/v1/players/data?name=${query}`)
            .then(res => res.json())
            .then(data => {
                setPlayers(data);
                setHasSearched(true);
            });
    };

    return (
        <div className="search-container">
            <h2 style={{ textAlign: 'center', marginBottom: '2rem', fontWeight: 300 }}>Pesquisar Jogador</h2>

            <div className="search-box">
                <input
                    type="text"
                    placeholder="Nome do jogador..."
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
                />
                <button className="search-button" onClick={handleSearch}>
                    Buscar
                </button>
            </div>

            {hasSearched && (
                <div style={{ marginTop: '2rem' }}>
                    <PlayerTable players={players} />
                </div>
            )}
        </div>
    );
}
export default SearchPage;