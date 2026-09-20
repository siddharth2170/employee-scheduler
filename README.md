# Employee Schedule Manager — Python and Java

This project implements the same command-line scheduling application in two contrasting languages. It demonstrates conditionals, `for`/`while` loops, branching, collections, validation, classes, and functions/methods.

## Rules implemented

- Seven-day schedule with morning, afternoon, and evening shifts.
- Exactly two employees per shift (the minimum required by the prompt).
- No employee can work twice on the same day.
- No employee can work more than five days in one week.
- Ranked preferences are supported as the optional bonus.
- When a preferred shift is full, the scheduler considers another ranked shift that day; employees remain eligible on following days.
- Ties are randomly resolved with a fixed seed, making the sample output reproducible.
- Both versions validate the completed schedule before displaying it.

Because 7 days × 3 shifts × 2 employees = 42 assignments and one employee can cover at most 5, the program requires at least 9 employees.

## Run Python

Requires Python 3.9 or newer.

```bash
cd python
python3 employee_scheduler.py
```

## Run Java

Requires JDK 11 or newer.

```bash
cd java
javac EmployeeScheduler.java
java EmployeeScheduler
```

At the prompt, enter `S` for the included nine-person sample or `M` to enter employee names and ranked daily preferences. Pressing Enter also selects sample mode.
