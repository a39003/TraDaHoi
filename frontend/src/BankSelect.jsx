import { createElement, useEffect, useMemo, useRef, useState } from 'react';

const React = { createElement };

// Danh sách ngân hàng hỗ trợ chuyển khoản VietQR. BIN được lưu ngầm,
// thành viên chỉ cần tìm kiếm và chọn tên ngân hàng.
export const BANKS = [
  { code: 'ICB', bin: '970415', shortName: 'VietinBank', name: 'Ngân hàng TMCP Công thương Việt Nam' },
  { code: 'VCB', bin: '970436', shortName: 'Vietcombank', name: 'Ngân hàng TMCP Ngoại thương Việt Nam' },
  { code: 'BIDV', bin: '970418', shortName: 'BIDV', name: 'Ngân hàng TMCP Đầu tư và Phát triển Việt Nam' },
  { code: 'VBA', bin: '970405', shortName: 'Agribank', name: 'Ngân hàng Nông nghiệp và Phát triển Nông thôn Việt Nam' },
  { code: 'MB', bin: '970422', shortName: 'MBBank', name: 'Ngân hàng TMCP Quân đội' },
  { code: 'TCB', bin: '970407', shortName: 'Techcombank', name: 'Ngân hàng TMCP Kỹ thương Việt Nam' },
  { code: 'ACB', bin: '970416', shortName: 'ACB', name: 'Ngân hàng TMCP Á Châu' },
  { code: 'VPB', bin: '970432', shortName: 'VPBank', name: 'Ngân hàng TMCP Việt Nam Thịnh Vượng' },
  { code: 'TPB', bin: '970423', shortName: 'TPBank', name: 'Ngân hàng TMCP Tiên Phong' },
  { code: 'OCB', bin: '970448', shortName: 'OCB', name: 'Ngân hàng TMCP Phương Đông' },
  { code: 'STB', bin: '970403', shortName: 'Sacombank', name: 'Ngân hàng TMCP Sài Gòn Thương Tín' },
  { code: 'HDB', bin: '970437', shortName: 'HDBank', name: 'Ngân hàng TMCP Phát triển TP. Hồ Chí Minh' },
  { code: 'VCCB', bin: '970454', shortName: 'VietCapitalBank', name: 'Ngân hàng TMCP Bản Việt' },
  { code: 'SCB', bin: '970429', shortName: 'SCB', name: 'Ngân hàng TMCP Sài Gòn' },
  { code: 'VIB', bin: '970441', shortName: 'VIB', name: 'Ngân hàng TMCP Quốc tế Việt Nam' },
  { code: 'SHB', bin: '970443', shortName: 'SHB', name: 'Ngân hàng TMCP Sài Gòn - Hà Nội' },
  { code: 'EIB', bin: '970431', shortName: 'Eximbank', name: 'Ngân hàng TMCP Xuất Nhập khẩu Việt Nam' },
  { code: 'MSB', bin: '970426', shortName: 'MSB', name: 'Ngân hàng TMCP Hàng Hải Việt Nam' },
  { code: 'CAKE', bin: '546034', shortName: 'CAKE', name: 'Ngân hàng số CAKE by VPBank' },
  { code: 'Ubank', bin: '546035', shortName: 'Ubank', name: 'Ngân hàng số Ubank by VPBank' },
  { code: 'TIMO', bin: '963388', shortName: 'Timo', name: 'Ngân hàng số Timo' },
  { code: 'SGICB', bin: '970400', shortName: 'SaigonBank', name: 'Ngân hàng TMCP Sài Gòn Công Thương' },
  { code: 'BAB', bin: '970409', shortName: 'BacABank', name: 'Ngân hàng TMCP Bắc Á' },
  { code: 'PVCB', bin: '970412', shortName: 'PVcomBank', name: 'Ngân hàng TMCP Đại Chúng Việt Nam' },
  { code: 'PVDB', bin: '971133', shortName: 'PVcomBank Pay', name: 'Ngân hàng số PVcomBank Pay' },
  { code: 'MBV', bin: '970414', shortName: 'MBV', name: 'Ngân hàng TNHH MTV Việt Nam Hiện Đại' },
  { code: 'NCB', bin: '970419', shortName: 'NCB', name: 'Ngân hàng TMCP Quốc Dân' },
  { code: 'SHBVN', bin: '970424', shortName: 'ShinhanBank', name: 'Ngân hàng TNHH MTV Shinhan Việt Nam' },
  { code: 'ABB', bin: '970425', shortName: 'ABBANK', name: 'Ngân hàng TMCP An Bình' },
  { code: 'VAB', bin: '970427', shortName: 'VietABank', name: 'Ngân hàng TMCP Việt Á' },
  { code: 'NAB', bin: '970428', shortName: 'NamABank', name: 'Ngân hàng TMCP Nam Á' },
  { code: 'PGB', bin: '970430', shortName: 'PGBank', name: 'Ngân hàng TMCP Thịnh Vượng và Phát triển' },
  { code: 'VIETBANK', bin: '970433', shortName: 'VietBank', name: 'Ngân hàng TMCP Việt Nam Thương Tín' },
  { code: 'BVB', bin: '970438', shortName: 'BaoVietBank', name: 'Ngân hàng TMCP Bảo Việt' },
  { code: 'SEAB', bin: '970440', shortName: 'SeABank', name: 'Ngân hàng TMCP Đông Nam Á' },
  { code: 'COOPBANK', bin: '970446', shortName: 'COOPBANK', name: 'Ngân hàng Hợp tác xã Việt Nam' },
  { code: 'LPB', bin: '970449', shortName: 'LPBank', name: 'Ngân hàng TMCP Lộc Phát Việt Nam' },
  { code: 'KLB', bin: '970452', shortName: 'KienLongBank', name: 'Ngân hàng TMCP Kiên Long' },
  { code: 'KBank', bin: '668888', shortName: 'KBank', name: 'Ngân hàng Đại chúng TNHH Kasikornbank' },
  { code: 'CIMB', bin: '422589', shortName: 'CIMB', name: 'Ngân hàng TNHH MTV CIMB Việt Nam' },
  { code: 'WVN', bin: '970457', shortName: 'Woori', name: 'Ngân hàng TNHH MTV Woori Việt Nam' },
  { code: 'momo', bin: '971025', shortName: 'MoMo', name: 'Ví điện tử MoMo' },
];

export default function BankSelect({ value, bankBin, onChange }) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const rootRef = useRef(null);
  const selected = BANKS.find((bank) => bank.bin === bankBin || normalize(bank.shortName) === normalize(value));
  const filtered = useMemo(() => {
    const keyword = normalize(query);
    if (!keyword) return BANKS;
    return BANKS.filter((bank) => normalize(`${bank.shortName} ${bank.name} ${bank.code}`).includes(keyword));
  }, [query]);

  useEffect(() => {
    if (!open) return undefined;
    const close = (event) => { if (!rootRef.current?.contains(event.target)) setOpen(false); };
    const escape = (event) => { if (event.key === 'Escape') setOpen(false); };
    document.addEventListener('mousedown', close);
    document.addEventListener('keydown', escape);
    return () => {
      document.removeEventListener('mousedown', close);
      document.removeEventListener('keydown', escape);
    };
  }, [open]);

  const choose = (bank) => {
    onChange({ bankName: bank.shortName, bankBin: bank.bin });
    setQuery('');
    setOpen(false);
  };

  return <div className="bank-select-field" ref={rootRef}>
    <span className="bank-select-label">Ngân hàng</span>
    <button type="button" className={`bank-select-trigger ${open ? 'open' : ''}`} onClick={() => setOpen(!open)} aria-expanded={open}>
      <span className="bank-code-mark">{selected?.code || '🏦'}</span>
      <span><strong>{selected?.shortName || value || 'Chọn ngân hàng'}</strong><small>{selected?.name || (value ? 'Hãy chọn lại ngân hàng trong danh sách' : 'Tìm theo tên hoặc tên viết tắt')}</small></span>
      <b>⌄</b>
    </button>
    {open && <div className="bank-select-popover">
      <label className="bank-search"><span>⌕</span><input autoFocus value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm Vietcombank, MB, Techcombank..." /></label>
      <div className="bank-option-list">
        {filtered.length === 0 && <p className="empty">Không tìm thấy ngân hàng phù hợp.</p>}
        {filtered.map((bank) => <button type="button" className={selected?.bin === bank.bin ? 'selected' : ''} key={bank.bin} onClick={() => choose(bank)}>
          <span className="bank-code-mark">{bank.code}</span><span><strong>{bank.shortName}</strong><small>{bank.name}</small></span>{selected?.bin === bank.bin && <b>✓</b>}
        </button>)}
      </div>
    </div>}
  </div>;
}

function normalize(value) {
  return String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('vi').replace(/[^a-z0-9]/g, '');
}
