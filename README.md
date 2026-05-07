# do-fast-leave-fast
## AuctionHub

Ung dung dau gia client JavaFX hoan chinh theo bai tap lon, gom:

- Dang nhap theo 3 vai tro `BIDDER`, `SELLER`, `ADMIN`
- Dang ky tai khoan moi cho `BIDDER` va `SELLER`
- Marketplace duyet phien dau gia, loc theo danh muc, xem chi tiet
- Dat gia, xem lich su bid, chart gia va thong bao
- Auto bid, anti-snipe, nap vi va thanh toan lot thang dau gia
- Seller Studio tao phien moi, quan ly lot dang ban va xem thong bao
- Admin Control xem thong ke he thong, user, payments, transactions va notifications
- Backend `server` va du lieu duoc luu ben vung trong thu muc `data`

### Tai khoan mau

- `bidder / bidder123`
- `seller / seller123`
- `admin / admin`

### Chay nhanh tren Windows

Chay trong PowerShell tai thu muc goc du an:

```powershell
.\run-app.ps1
```

Script se:

1. Bien dich `client/java` va `server/src/main`
2. Copy `client/resources`
3. Chay `ClientMain`

### PostgreSQL / Supabase

Project da duoc chuyen sang dung PostgreSQL lam nguon du lieu chung. Moi may chi can chay app, app se tu goi logic backend noi bo va doc/ghi truc tiep len cloud database.

App se tu dong:

- ket noi toi PostgreSQL
- tao bang neu database dang rong
- import du lieu cu tu `data/*.dat` len database trong lan chay dau tien neu co

Can cau hinh database bang 1 trong 3 cach:

1. Tao file `config/database.properties` dua tren `config/database.properties.example`
2. Truyen `-Dauction.db.*` khi chay Java
3. Dat bien moi truong `AUCTION_DB_*`

Chay app tren bat ky may nao:

```powershell
.\run-app.ps1 -DbHost db.zvmrojzygaurnxakqxlu.supabase.co -DbPort 5432 -DbName postgres -DbUser postgres -DbPassword "your-password"
```

Neu da tao `config/database.properties` thi chi can:

```powershell
.\run-app.ps1
```

Neu muon chay bang Maven, can cai Maven vao `PATH` truoc.

### Kich ban test thu cong

1. Dang nhap `bidder / bidder123`, vao chi tiet lot bat ky va dat gia.
2. Trong cung man hinh chi tiet, bat `Auto bid` voi muc tran lon hon gia hien tai.
3. Dang nhap `seller / seller123`, tao mot lot moi trong `Seller Studio`.
4. Dang nhap `admin / admin123`, vao `Admin Control` de xem `Payments`, `Transactions`, `Notifications`.
5. Dung lot da ket thuc ma bidder dang thang de test `Nap tien vao vi` va `Thanh toan lot nay`.

### Luu du lieu

- User: `data/users.dat`
- Auction: `data/auctions.dat`
- Payment: `data/payments.dat`
- Transaction log: `data/transactions.dat`
- Notification: `data/notifications.dat`

Du lieu nay duoc backend trong `server` doc/ghi tu dong. Sau khi tat app, mo lai van giu trang thai.
