use tauri::command;

#[command]
pub fn get_minecraft_path() -> String {
    let appdata = std::env::var("APPDATA").unwrap_or_default();
    format!(r"{}\.minecraft", appdata)
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .invoke_handler(tauri::generate_handler![get_minecraft_path])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}