# AuctionHub

Ung dung dau gia JavaFX theo kien truc Client-Server.

## Chuc nang

- Dang nhap theo 3 vai tro: Bidder, Seller, Admin
- Dang ky tai khoan Bidder va Seller
- CRUD phien dau gia cho Seller
- Seller chon file anh san pham PNG, JPG, GIF hoac BMP toi da 2 MB
- Dat gia realtime, auto-bidding va anti-sniping
- Tu dong dong phien, xac dinh nguoi thang va thanh toan
- Nap tien qua yeu cau cho Admin phe duyet
- Admin quan ly user, giao dich, thanh toan va thong bao
- Bieu do lich su gia realtime

## Kien truc

- `client -> TCP socket JSON line protocol -> server -> PostgreSQL`
- Server chay `server.ServerLauncherMain` va la noi duy nhat truy cap database
- Client JavaFX dung controller theo huong MVC va nhan `RealtimeEvent` theo Observer Pattern
- `ServerMain` dung Singleton Pattern
- `ItemController` dung Factory Method de tao `Electronics`, `Vehicle`, `Art` va `GenericItem`
- Auto-bidding dung `PriorityQueue` de uu tien rule co muc tran cao nhat
- Login va dang ky dung FXML that; controller chi xu ly su kien va goi service
- `data/*.dat` chi dung de migrate du lieu legacy khi database rong

## Tai khoan mau

- `bidder / bidder123`
- `seller / seller123`
- `admin / admin`

## Chay tren Windows

Neu da tao `config/database.properties`:

```powershell
.\run-server.ps1
```

Mo terminal khac va chay client tren cung may:

```powershell
.\run-app.ps1
```

Neu dung ngrok TCP cho client tu may khac:

```powershell
ngrok tcp 5050
.\run-app.ps1 -ServerHost <host-ngrok> -Port <port-ngrok>
```

## Chay tren macOS / Linux

```bash
chmod +x run-server-macos.sh run-app-macos.sh run-server-linux.sh run-app-linux.sh
./run-server-linux.sh
./run-app-linux.sh --server-host localhost --port 5050
```

Tren macOS thay hau to `linux` bang `macos`.

## Cau hinh database

Tao `config/database.properties` dua tren `config/database.properties.example`, hoac truyen system properties `auction.db.*`, hoac dung bien moi truong `AUCTION_DB_*`. File `config/database.properties` duoc bo qua boi Git de khong day credential len repository.

## Test va CI

Workflow nam tai `.github/workflows/java-ci.yml`.

```powershell
mvn verify
```

Lenh nay chay:

- JUnit 5
- Integration test TCP socket cho request loi, subscribe token va realtime event
- JaCoCo coverage report
- JaCoCo gate yeu cau coverage domain `server.model.*` tu 60% tro len
- Checkstyle coding convention

Bao cao coverage day du nam tai `target/site/jacoco/index.html`.
