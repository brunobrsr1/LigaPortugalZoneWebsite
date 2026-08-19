import { Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar';
import Home from './pages/Home';
import CategoryPage from './pages/CategoryPage';
import SearchPage from './pages/SearchPage';
import './App.css';

function App() {
    return (
        <div className="app-container">
            <Navbar />
            <div className="container">
                <Routes>
                    <Route path="/" element={<Home />} />
                    <Route path="/teams" element={<CategoryPage key="team" type="team" />} />
                    <Route path="/nations" element={<CategoryPage key="nation" type="nation" />} />
                    <Route path="/positions" element={<CategoryPage key="position" type="position" />} />
                    <Route path="/search" element={<SearchPage />} />
                </Routes>
            </div>
        </div>
    );
}

export default App;