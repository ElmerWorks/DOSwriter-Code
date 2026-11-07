# DOSwriter Remote Browser Architecture & Implementation Summary

## 1. Overview
The **ElmerSync Remote File Browser** enables DOSwriter on Android to securely connect to a desktop host running `elmer-syncd`, browse folder and subfolder directory listings relative to the server launch root, open remote text files into active editing buffers, and save edited changes back to the host server.

---

## 2. Client Networking & Parsing (`RemoteBrowserClient.kt`)
Located in `com.example.doswriter.logic.RemoteBrowserClient`, this module implements the HTTP client contract interacting with the Go host server:
- **`listDir(path: String)`**: Issues `GET /api/list?path=<encoded-path>` with optional `X-API-Token` authentication header, returning a `RemoteListResponse` containing `[RemoteEntry]` (name, isDir, size, mtime).
- **`readFile(path: String)`**: Issues `GET /api/read?path=<encoded-path>` to fetch raw file contents from the host.
- **`writeFile(path: String, content: String)`**: Issues `POST /api/write` with a JSON payload (`{"path": "...", "content": "..."}`) to persist changes atomically on the host.

---

## 3. Navigation & Path Resolution
Path safety and tree traversal are managed directly within `RemoteBrowserClient`:
- **Current Path State**: Tracks `currentPath` (defaulting to `.` for the server root).
- **Subdirectory Resolution (`resolveSubPath`)**: Safely appends child directories relative to the current working root.
- **Parent Navigation (`resolveParentPath`)**: Handles `..` parent folder traversal while enforcing root-scoped security boundaries.

---

## 4. UI Binding & Overlay Integration (`CTRL-ALT-F` / `OpenFileBrowser`)
The remote browser is integrated directly into DOSwriter's file picker flow (`OverlayHandler.kt`):
- When `CTRL-ALT-F` (`OpenFileBrowser`) is invoked, the app checks if a remote host server is connected (`syncManager.isConnected()`).
- If connected, it presents a unified selection pick list combining local working folders with the remote host:
  ```text
  [FILE] Code
  [REMOTE] 192.168.1.50
  ```
- Selecting `[REMOTE]` launches `showRemoteFilePicker` which queries the server (`/api/list`), allows hierarchical folder navigation, and opens selected files into the editor buffer via WebSocket sync (`syncManager.openRemoteFile`).

---

## 5. Security & Safety Rules
- **Root Boundary**: Host server operations are strictly restricted to the terminal launch directory or specified `--root` subtree.
- **Path Traversal Protection**: Absolute paths and traversal attempts above root (`..`) are normalized and blocked.
- **Authentication**: All requests require a valid `X-API-Token` header matching the server configuration.
