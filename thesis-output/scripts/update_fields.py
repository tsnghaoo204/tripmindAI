"""Mở DOCX bằng LibreOffice headless, cập nhật Mục lục/Danh mục hình/bảng + trường, lưu lại DOCX và xuất PDF."""
import os
import subprocess
import sys
import time
from pathlib import Path

import uno
from com.sun.star.beans import PropertyValue

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "report" / "final-report.docx"
PDF = ROOT / "report" / "final-report.pdf"
PORT = 2093


def prop(name, value):
    p = PropertyValue()
    p.Name, p.Value = name, value
    return p


def main():
    profile = Path(os.environ.get("TMPDIR", "/tmp")) / "lo_thesis_profile"
    proc = subprocess.Popen(["soffice", "--headless", "--invisible", "--norestore", "--nologo",
                             f"-env:UserInstallation=file://{profile}",
                             f"--accept=socket,host=127.0.0.1,port={PORT};urp;"],
                            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    try:
        local = uno.getComponentContext()
        resolver = local.ServiceManager.createInstanceWithContext("com.sun.star.bridge.UnoUrlResolver", local)
        ctx = None
        for _ in range(60):
            try:
                ctx = resolver.resolve(f"uno:socket,host=127.0.0.1,port={PORT};urp;StarOffice.ComponentContext")
                break
            except Exception:
                time.sleep(0.5)
        if ctx is None:
            print("Không kết nối được LibreOffice")
            return 1
        desktop = ctx.ServiceManager.createInstanceWithContext("com.sun.star.frame.Desktop", ctx)
        doc = desktop.loadComponentFromURL(uno.systemPathToFileUrl(str(SRC)), "_blank", 0, (prop("Hidden", True),))
        # cập nhật 2 lượt: lượt 2 lấy đúng số trang sau khi mục lục đã chiếm chỗ
        for _ in range(2):
            idx = doc.getDocumentIndexes()
            for k in range(idx.getCount()):
                idx.getByIndex(k).update()
            doc.getTextFields().refresh()
            doc.refresh()
        n = doc.getDocumentIndexes().getCount()
        doc.storeToURL(uno.systemPathToFileUrl(str(SRC)), (prop("FilterName", "MS Word 2007 XML"),))
        doc.storeToURL(uno.systemPathToFileUrl(str(PDF)), (prop("FilterName", "writer_pdf_Export"),))
        doc.close(True)
        print(f"Đã cập nhật {n} chỉ mục; lưu {SRC.name} và {PDF.name}")
        return 0
    finally:
        try:
            desktop.terminate()
        except Exception:
            pass
        proc.terminate()


if __name__ == "__main__":
    sys.exit(main())
