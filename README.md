# do-fast-leave-fast
## AuctionHub

Auction app hoan chinh gom:

- Dang nhap theo 3 vai tro Bidder, Seller, Admin
- Dang ky tai khoan moi cho Bidder va Seller
- Marketplace duyet phien dau gia, loc theo danh muc, xem chi tiet
- Dat gia, xem lich su bid, chart gia va thong bao
- Auto bid, anti-snipe, nap vi va thanh toan lot thang dau gia
- Seller Studio tao phien moi, quan ly lot dang ban va xem thong bao
- Admin Control xem thong ke he thong, user, payments, transactions va notifications

### Kien truc hien tai

- `client -> socket server -> PostgreSQL`
- May server chay `server.ServerLauncherMain` va la diem duy nhat truy cap database
- Cac may client chi ket noi toi `auction.server.host` / `auction.server.port`
- Giao thuc client-server dung `socket TCP + JSON line protocol`
- Client giao dien dung JavaFX va FXML, controller theo huong MVC
- Thu muc `data/*.dat` chi con dung de migrate du lieu cu len database trong lan khoi dong dau tien khi database dang rong

### Tai khoan mau

- `bidder / bidder123`
- `seller / seller123`
- `admin / admin`

### Chay tren Windows

Chay server tren may dung lam backend chung:

```powershell
.\run-server.ps1 -Port 5050 -DbHost your-db-host -DbPort 5432 -DbName postgres -DbUser postgres -DbPassword "your-password"
```

Neu da tao `config/database.properties` thi chi can:

```powershell
.\run-server.ps1
```

Sau do, tren moi may client:

```powershell
.\run-app.ps1 -ServerHost 192.168.1.10 -Port 5050
```

`run-app.ps1` se:

1. Bien dich `client/java` va `src/main/java`
2. Copy `client/resources`
3. Chay `ClientMain` va ket noi toi server chung

### PostgreSQL / Supabase

Project da duoc chuyen sang dung PostgreSQL lam nguon du lieu chung. Client khong truy cap database truc tiep; moi thao tac deu di qua socket server.

Server se tu dong:

- ket noi toi PostgreSQL
- tao bang neu database dang rong
- import du lieu cu tu `data/*.dat` len database trong lan chay dau tien neu co

Can cau hinh database bang 1 trong 3 cach tren may server:

1. Tao file `config/database.properties` dua tren `config/database.properties.example`
2. Truyen `-Dauction.db.*` khi chay Java
3. Dat bien moi truong `AUCTION_DB_*`

### Real-time update

Cap nhat du lieu da chuyen sang co che event-driven:

- Moi client mo mot kenh subscribe den server bang action `SUBSCRIBE_EVENTS`
- Server phat `RealtimeEvent` dang JSON moi khi co thay doi quan trong
- `AuctionList`, `AuctionDetail`, `Seller`, `Wallet`, `Admin` se refresh theo su kien thay vi polling chu ky
- Bid history chart hien thi truc X theo timestamp cua tung bid

### Kich ban test thu cong

1. Chay `run-server.ps1` tren may server.
2. Chay `run-app.ps1 -ServerHost <ip-server>` tren 2 may client khac nhau.
3. Dang nhap `bidder / bidder123` o client 1, vao chi tiet lot bat ky va dat gia.
4. O client 2, vao danh sach lot hoac vi de kiem tra du lieu cap nhat ngay sau khi server phat event.
5. Dang nhap `seller / seller123`, tao mot lot moi trong `Seller Studio`.
6. Dang nhap `admin / admin`, vao `Admin Control` de xem `Payments`, `Transactions`, `Notifications`.

### Unit test va CI

- Unit test dung JUnit 5 trong `src/test/java`
- CI workflow nam tai `.github/workflows/java-ci.yml`
- Lenh local:

```powershell
mvn test
```

### Luu du lieu

- Nguon du lieu chinh: PostgreSQL
- `data/users.dat`, `data/auctions.dat`, `data/payments.dat`, `data/transactions.dat`, `data/notifications.dat`, `data/topup-requests.dat`: chi dung de migrate legacy data
