@echo off
REM Sets the default max heap to 1024 memory for 32 bit systems 
rem or 8192 for 64 bit system.s
SETLOCAL 
if "%BSW_MEM%"=="" goto set_mem
goto mem_is_set

:set_mem
if "%ProgramW6432%"=="" goto set_32
SET BSW_MEM=8192M
goto mem_is_set

:set_32
SET BSW_MEM=1024M

:mem_is_set
java -Xmx%BSW_MEM% -jar %0\..\BookScanWizard.jar %*

