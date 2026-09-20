#!/bin/sh
set -eu

printf 's\n' | python3 python/employee_scheduler.py > python/sample-output.txt
javac java/EmployeeScheduler.java
printf 's\n' | java -cp java EmployeeScheduler > java/sample-output.txt

echo "Both implementations ran successfully and validated their schedules."
