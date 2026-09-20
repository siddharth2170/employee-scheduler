"""Weekly employee scheduler demonstrating conditionals, loops, and branching."""

from __future__ import annotations

import random
from dataclasses import dataclass, field

DAYS = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
SHIFTS = ["morning", "afternoon", "evening"]
STAFF_PER_SHIFT = 2
MAX_DAYS = 5


@dataclass
class Employee:
    name: str
    # Each day stores ranked choices; index 0 is the first choice.
    preferences: dict[str, list[str]]
    days_worked: int = 0
    assignments: dict[str, str] = field(default_factory=dict)


def collect_employees() -> list[Employee]:
    """Collect a roster and optional ranked preferences from the keyboard."""
    while True:
        try:
            count = int(input("Number of employees (minimum 9): "))
            if count >= 9:
                break
        except ValueError:
            pass
        print("Please enter a whole number of at least 9.")

    employees: list[Employee] = []
    for number in range(1, count + 1):
        name = input(f"Employee {number} name: ").strip()
        while not name or any(employee.name.lower() == name.lower() for employee in employees):
            name = input("Enter a nonempty, unique name: ").strip()

        preferences: dict[str, list[str]] = {}
        print("Enter ranked shifts separated by commas (example: morning,evening,afternoon).")
        for day in DAYS:
            while True:
                choices = [item.strip().lower() for item in input(f"  {day}: ").split(",")]
                choices = [item for item in choices if item]
                if choices and len(choices) == len(set(choices)) and all(item in SHIFTS for item in choices):
                    preferences[day] = choices
                    break
                print("  Use one or more unique choices: morning, afternoon, evening.")
        employees.append(Employee(name, preferences))
    return employees


def sample_employees() -> list[Employee]:
    """Create repeatable demonstration data with ranked preferences."""
    names = ["Ava", "Ben", "Chloe", "Diego", "Emma", "Finn", "Grace", "Hassan", "Ivy"]
    rotations = [
        ["morning", "afternoon", "evening"],
        ["afternoon", "evening", "morning"],
        ["evening", "morning", "afternoon"],
    ]
    employees = []
    for employee_index, name in enumerate(names):
        preferences = {}
        for day_index, day in enumerate(DAYS):
            preferences[day] = rotations[(employee_index + day_index) % len(rotations)].copy()
        employees.append(Employee(name, preferences))
    return employees


def preference_rank(employee: Employee, day: str, shift: str) -> int:
    choices = employee.preferences.get(day, [])
    return choices.index(shift) if shift in choices else len(SHIFTS) + 1


def make_schedule(employees: list[Employee], seed: int = 42) -> dict[str, dict[str, list[str]]]:
    """Assign exactly two people per shift while respecting daily/weekly limits."""
    if len(employees) * MAX_DAYS < len(DAYS) * len(SHIFTS) * STAFF_PER_SHIFT:
        raise ValueError("At least 9 employees are needed to cover 42 shifts within the five-day limit.")

    rng = random.Random(seed)
    schedule = {day: {shift: [] for shift in SHIFTS} for day in DAYS}

    for day_index, day in enumerate(DAYS):
        # Lowest work count first prevents early employees from reaching five days too soon.
        eligible = [employee for employee in employees if employee.days_worked < MAX_DAYS]
        rng.shuffle(eligible)
        eligible.sort(key=lambda employee: employee.days_worked)

        for shift in SHIFTS:
            while len(schedule[day][shift]) < STAFF_PER_SHIFT:
                available = [employee for employee in eligible if day not in employee.assignments]
                if not available:
                    raise RuntimeError(f"Unable to cover {day} {shift}; add more employees.")

                # Prefer this shift, then favor the person with fewer total days.
                available.sort(
                    key=lambda employee: (preference_rank(employee, day, shift), employee.days_worked)
                )
                best_rank = preference_rank(available[0], day, shift)
                tied = [
                    employee
                    for employee in available
                    if preference_rank(employee, day, shift) == best_rank
                    and employee.days_worked == available[0].days_worked
                ]
                chosen = rng.choice(tied)
                schedule[day][shift].append(chosen.name)
                chosen.assignments[day] = shift
                chosen.days_worked += 1

        # This branch documents conflicts: employees not selected for a full preferred
        # shift remain eligible for another shift today, or naturally for the next day.
        assert all(len(schedule[day][shift]) == STAFF_PER_SHIFT for shift in SHIFTS)

    return schedule


def validate_schedule(employees: list[Employee], schedule: dict[str, dict[str, list[str]]]) -> None:
    """Raise AssertionError if any assignment rule is broken."""
    totals = {employee.name: 0 for employee in employees}
    for day in DAYS:
        assigned_today: list[str] = []
        for shift in SHIFTS:
            names = schedule[day][shift]
            assert len(names) >= STAFF_PER_SHIFT
            assigned_today.extend(names)
            for name in names:
                totals[name] += 1
        assert len(assigned_today) == len(set(assigned_today))
    assert all(total <= MAX_DAYS for total in totals.values())


def print_schedule(schedule: dict[str, dict[str, list[str]]], employees: list[Employee]) -> None:
    print("\nFINAL WEEKLY SCHEDULE")
    print("=" * 64)
    for day in DAYS:
        print(f"\n{day}")
        for shift in SHIFTS:
            print(f"  {shift.title():<10}: {', '.join(schedule[day][shift])}")
    print("\nDAYS WORKED")
    print("-" * 64)
    for employee in sorted(employees, key=lambda item: item.name):
        print(f"  {employee.name:<10}: {employee.days_worked}")


def main() -> None:
    choice = input("Use [S]ample data or [M]anual entry? ").strip().lower()
    employees = collect_employees() if choice.startswith("m") else sample_employees()
    schedule = make_schedule(employees)
    validate_schedule(employees, schedule)
    print_schedule(schedule, employees)


if __name__ == "__main__":
    main()
