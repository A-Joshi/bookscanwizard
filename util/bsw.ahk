;
; Autohotkey (www.autohotkey.com) script that ocr's and coverts a directory
; of tiff files to a pdf file.  It uses version 10 of ABBYY
; FineReader professional.
;
SetTitleMatchMode 2 
run C:\Program Files\ABBYY FineReader 10\FineReader
WinWaitActive ABBYY FineReader
Send ^t
Send !n
WinWaitActive, A_Convert, Close
Send {Enter}
WinWaitNotActive, A_Convert, Close
Send !fx
WinWaitActive,, Do you want to save
Send n
return
