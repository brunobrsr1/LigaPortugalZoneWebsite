import { Link, useLocation } from 'react-router-dom';
import { LayoutGrid, Users, Map, Trophy, Search } from 'lucide-react';

function Navbar() {
    const location = useLocation();
    const isActive = (path) => location.pathname === path ? 'active-link' : '';

    return (
        <nav className="navbar">
            <Link to="/" className="nav-logo-link">
                <img src="/logo.png" alt="Logo LigaZone" className="navbar-logo-img" />

                <span className="nav-logo">LigaZone</span>
            </Link>

            <div className="nav-links">
                <Link to="/" className={isActive('/')}>
                    <LayoutGrid size={18} strokeWidth={1.5} />
                    <span className="link-text">Home</span>
                </Link>
                <Link to="/teams" className={isActive('/teams')}>
                    <Trophy size={18} strokeWidth={1.5} />
                    <span className="link-text">Equipas</span>
                </Link>
                <Link to="/nations" className={isActive('/nations')}>
                    <Map size={18} strokeWidth={1.5} />
                    <span className="link-text">Países</span>
                </Link>
                <Link to="/positions" className={isActive('/positions')}>
                    <Users size={18} strokeWidth={1.5} />
                    <span className="link-text">Posições</span>
                </Link>
                <Link to="/search" className={`search-btn-nav ${isActive('/search')}`}>
                    <Search size={18} strokeWidth={1.5} />
                </Link>
            </div>
        </nav>
    );
}

export default Navbar;