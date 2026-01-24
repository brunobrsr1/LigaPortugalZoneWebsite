import { Link } from 'react-router-dom';
import { Shield, Globe, User, Search } from 'lucide-react';

function Home() {
    return (
        <div className="hero-section">
            <h1 className="hero-title">Bem-vindo à Liga Portugal Zone!</h1>
            <p className="hero-subtitle">A tua casa para tudo o relacionado à Liga Portugal.</p>

            <div className="stats-grid">
                <Link to="/teams" style={{ textDecoration: 'none' }}>
                    <div className="feature-card">
                        <Shield size={32} color="#94a3b8" strokeWidth={1.5} />
                        <h3>Equipas</h3>
                        <p>Plantéis completos</p>
                    </div>
                </Link>

                <Link to="/positions" style={{ textDecoration: 'none' }}>
                    <div className="feature-card">
                        <User size={32} color="#94a3b8" strokeWidth={1.5} />
                        <h3>Posições</h3>
                        <p>Análise por função</p>
                    </div>
                </Link>

                <Link to="/nations" style={{ textDecoration: 'none' }}>
                    <div className="feature-card">
                        <Globe size={32} color="#94a3b8" strokeWidth={1.5} />
                        <h3>Nacionalidades</h3>
                        <p>Origem dos talentos</p>
                    </div>
                </Link>

                <Link to="/search" style={{ textDecoration: 'none' }}>
                    <div className="feature-card">
                        <Search size={32} color="#94a3b8" strokeWidth={1.5} />
                        <h3>Pesquisa</h3>
                        <p>Encontrar jogador</p>
                    </div>
                </Link>
            </div>
        </div>
    );
}

export default Home;