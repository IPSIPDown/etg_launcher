import { useState } from "react";
import { invoke } from "@tauri-apps/api/core"; // В Tauri v2 импорт выглядит так
import "./App.css";

function App() {
  const [username, setUsername] = useState("");
  const [status, setStatus] = useState("Готов к запуску");

  async function handlePlay() {
    if (!username) {
      setStatus("Введи никнейм!");
      return;
    }
    
    setStatus("Запуск...");
    try {
      // Обращаемся к Rust-функции launch_game
      const response = await invoke("launch_game", { player: username });
      setStatus(response as string);
    } catch (error) {
      setStatus(`Ошибка: ${error}`);
    }
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', marginTop: '50px' }}>
      <h1>EternalCraft</h1>
      
      <input
        type="text"
        placeholder="Твой ник"
        value={username}
        onChange={(e) => setUsername(e.currentTarget.value)}
        style={{ padding: '10px', fontSize: '16px', marginBottom: '10px' }}
      />
      
      <button 
        onClick={handlePlay}
        style={{ padding: '10px 30px', fontSize: '18px', cursor: 'pointer' }}
      >
        Играть
      </button>
      
      <p style={{ marginTop: '20px', color: '#888' }}>{status}</p>
    </div>
  );
}

export default App;