import { Activity, Clock, AlertCircle } from 'lucide-react'; // Ícones opcionais para headers

function PlayerTable({ players }) {
    if (players.length === 0) return <div className="empty">Nenhum jogador encontrado.</div>;

    return (
        <div className="table-wrapper">
            <table>
                <thead>
                <tr>
                    <th title="Nome do Jogador">Jogador</th>
                    <th title="Equipa">Equipa</th>
                    <th title="Nacionalidade">Nac.</th>
                    <th title="Posição">Pos.</th>
                    <th title="Idade" className="center-text">Idade</th>

                    <th title="Jogos Realizados" className="center-text">Jogos</th>
                    <th title="Jogos como Titular" className="center-text">Titular</th>
                    <th title="Minutos Jogados" className="center-text">Min</th>

                    <th title="Golos Marcados" className="center-text">Golos</th>
                    <th title="Assistências" className="center-text">Ast</th>
                    <th title="Golos marcados por 90 minutos" className="center-text">Golos/90</th>
                    <th title="Assistências por cada 90 minutos" className="center-text">Ast/90</th>

                    <th title="Cartões Amarelos" className="center-text">🟨</th>
                    <th title="Cartões Vermelhos" className="center-text">🟥</th>
                </tr>
                </thead>
                <tbody>
                {players.map((player) => (
                    <tr key={player.id}>
                        <td className="player-name">
                            {player.name}
                            {player.pk > 0 && <span className="pk-badge" title={`${player.pk} penaltis`}>+{player.pk}PK</span>}
                        </td>
                        <td className="team-cell">{player.team}</td>
                        <td>{player.nation}</td>
                        <td className="pos-badge">{player.position}</td>
                        <td className="center-text text-muted">{player.age}</td>

                        <td className="center-text font-bold">{player.mp}</td>
                        <td className="center-text text-muted">{player.starts}</td>
                        <td className="center-text text-muted">
                            {player.min ? player.min.toLocaleString() : '-'}
                        </td>

                        <td className={`center-text ${player.gls > 0 ? 'stat-highlight' : 'text-muted'}`}>
                            {player.gls}
                        </td>
                        <td className={`center-text ${player.ast > 0 ? 'stat-highlight' : 'text-muted'}`}>
                            {player.ast}
                        </td>

                        <td className="center-text advanced-stat" title={`Golos: ${player.gls} | Média: ${player.goalsPer90}/90min`}>
                            {player.goalsPer90?.toFixed(2)}
                        </td>

                        <td className="center-text advanced-stat" title={`Assistências: ${player.ast} | Média: ${player.assistsPer90}/90min`}>
                            {player.assistsPer90?.toFixed(2)}
                        </td>

                        <td className={`center-text ${player.crdY > 4 ? 'text-warn' : ''}`}>{player.crdY}</td>
                        <td className={`center-text ${player.crdR > 0 ? 'text-danger' : ''}`}>{player.crdR}</td>
                    </tr>
                ))}
                </tbody>
            </table>
        </div>
    );
}

export default PlayerTable;