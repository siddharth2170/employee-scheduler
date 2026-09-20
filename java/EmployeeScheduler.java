import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

public class EmployeeScheduler {
    private static final List<String> DAYS = Arrays.asList(
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
    private static final List<String> SHIFTS = Arrays.asList("morning", "afternoon", "evening");
    private static final int STAFF_PER_SHIFT = 2;
    private static final int MAX_DAYS = 5;

    static class Employee {
        final String name;
        final Map<String, List<String>> preferences;
        final Map<String, String> assignments = new LinkedHashMap<>();
        int daysWorked = 0;

        Employee(String name, Map<String, List<String>> preferences) {
            this.name = name;
            this.preferences = preferences;
        }
    }

    static List<Employee> collectEmployees(Scanner scanner) {
        int count = 0;
        while (count < 9) {
            System.out.print("Number of employees (minimum 9): ");
            if (scanner.hasNextInt()) count = scanner.nextInt();
            else scanner.next();
            if (count < 9) System.out.println("Please enter a whole number of at least 9.");
        }
        scanner.nextLine();

        List<Employee> employees = new ArrayList<>();
        for (int number = 1; number <= count; number++) {
            String name;
            while (true) {
                System.out.print("Employee " + number + " name: ");
                name = scanner.nextLine().trim();
                final String proposedName = name;
                boolean duplicate = employees.stream().anyMatch(
                        employee -> employee.name.equalsIgnoreCase(proposedName));
                if (!name.isEmpty() && !duplicate) break;
                System.out.println("Enter a nonempty, unique name.");
            }

            Map<String, List<String>> preferences = new LinkedHashMap<>();
            System.out.println("Enter ranked shifts separated by commas.");
            for (String day : DAYS) {
                while (true) {
                    System.out.print("  " + day + ": ");
                    String[] pieces = scanner.nextLine().toLowerCase().split(",");
                    List<String> choices = new ArrayList<>();
                    for (String piece : pieces) {
                        String choice = piece.trim();
                        if (!choice.isEmpty()) choices.add(choice);
                    }
                    boolean valid = !choices.isEmpty()
                            && choices.stream().allMatch(SHIFTS::contains)
                            && choices.stream().distinct().count() == choices.size();
                    if (valid) {
                        preferences.put(day, choices);
                        break;
                    }
                    System.out.println("  Use unique choices: morning, afternoon, evening.");
                }
            }
            employees.add(new Employee(name, preferences));
        }
        return employees;
    }

    static List<Employee> sampleEmployees() {
        List<String> names = Arrays.asList("Ava", "Ben", "Chloe", "Diego", "Emma", "Finn", "Grace", "Hassan", "Ivy");
        List<List<String>> rotations = Arrays.asList(
                Arrays.asList("morning", "afternoon", "evening"),
                Arrays.asList("afternoon", "evening", "morning"),
                Arrays.asList("evening", "morning", "afternoon"));
        List<Employee> employees = new ArrayList<>();
        for (int employeeIndex = 0; employeeIndex < names.size(); employeeIndex++) {
            Map<String, List<String>> preferences = new LinkedHashMap<>();
            for (int dayIndex = 0; dayIndex < DAYS.size(); dayIndex++) {
                preferences.put(DAYS.get(dayIndex),
                        new ArrayList<>(rotations.get((employeeIndex + dayIndex) % rotations.size())));
            }
            employees.add(new Employee(names.get(employeeIndex), preferences));
        }
        return employees;
    }

    static int preferenceRank(Employee employee, String day, String shift) {
        List<String> choices = employee.preferences.getOrDefault(day, Collections.emptyList());
        int rank = choices.indexOf(shift);
        return rank >= 0 ? rank : SHIFTS.size() + 1;
    }

    static Map<String, Map<String, List<String>>> makeSchedule(List<Employee> employees, long seed) {
        int needed = DAYS.size() * SHIFTS.size() * STAFF_PER_SHIFT;
        if (employees.size() * MAX_DAYS < needed) {
            throw new IllegalArgumentException("At least 9 employees are needed to cover 42 shifts.");
        }

        Random random = new Random(seed);
        Map<String, Map<String, List<String>>> schedule = new LinkedHashMap<>();
        for (String day : DAYS) {
            Map<String, List<String>> daily = new LinkedHashMap<>();
            for (String shift : SHIFTS) daily.put(shift, new ArrayList<>());
            schedule.put(day, daily);
        }

        for (String day : DAYS) {
            List<Employee> eligible = new ArrayList<>();
            for (Employee employee : employees) {
                if (employee.daysWorked < MAX_DAYS) eligible.add(employee);
            }
            Collections.shuffle(eligible, random);
            eligible.sort(Comparator.comparingInt(employee -> employee.daysWorked));

            for (String shift : SHIFTS) {
                while (schedule.get(day).get(shift).size() < STAFF_PER_SHIFT) {
                    List<Employee> available = new ArrayList<>();
                    for (Employee employee : eligible) {
                        if (!employee.assignments.containsKey(day)) available.add(employee);
                    }
                    if (available.isEmpty()) {
                        throw new IllegalStateException("Unable to cover " + day + " " + shift);
                    }
                    available.sort(Comparator
                            .comparingInt((Employee employee) -> preferenceRank(employee, day, shift))
                            .thenComparingInt(employee -> employee.daysWorked));
                    int bestRank = preferenceRank(available.get(0), day, shift);
                    int bestDays = available.get(0).daysWorked;
                    List<Employee> tied = new ArrayList<>();
                    for (Employee employee : available) {
                        if (preferenceRank(employee, day, shift) == bestRank
                                && employee.daysWorked == bestDays) tied.add(employee);
                    }
                    Employee chosen = tied.get(random.nextInt(tied.size()));
                    schedule.get(day).get(shift).add(chosen.name);
                    chosen.assignments.put(day, shift);
                    chosen.daysWorked++;
                }
            }
        }
        return schedule;
    }

    static void validateSchedule(List<Employee> employees, Map<String, Map<String, List<String>>> schedule) {
        Map<String, Integer> totals = new LinkedHashMap<>();
        for (Employee employee : employees) totals.put(employee.name, 0);
        for (String day : DAYS) {
            List<String> assignedToday = new ArrayList<>();
            for (String shift : SHIFTS) {
                List<String> names = schedule.get(day).get(shift);
                if (names.size() < STAFF_PER_SHIFT) throw new AssertionError("Understaffed shift");
                assignedToday.addAll(names);
                for (String name : names) totals.put(name, totals.get(name) + 1);
            }
            if (assignedToday.stream().distinct().count() != assignedToday.size())
                throw new AssertionError("Employee assigned twice in one day");
        }
        for (int total : totals.values()) {
            if (total > MAX_DAYS) throw new AssertionError("Employee exceeds five days");
        }
    }

    static void printSchedule(Map<String, Map<String, List<String>>> schedule, List<Employee> employees) {
        System.out.println("\nFINAL WEEKLY SCHEDULE");
        System.out.println("================================================================");
        for (String day : DAYS) {
            System.out.println("\n" + day);
            for (String shift : SHIFTS) {
                String label = shift.substring(0, 1).toUpperCase() + shift.substring(1);
                System.out.printf("  %-10s: %s%n", label, String.join(", ", schedule.get(day).get(shift)));
            }
        }
        System.out.println("\nDAYS WORKED");
        System.out.println("----------------------------------------------------------------");
        employees.stream().sorted(Comparator.comparing(employee -> employee.name))
                .forEach(employee -> System.out.printf("  %-10s: %d%n", employee.name, employee.daysWorked));
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Use [S]ample data or [M]anual entry? ");
        String choice = scanner.nextLine().trim().toLowerCase();
        List<Employee> employees = choice.startsWith("m") ? collectEmployees(scanner) : sampleEmployees();
        Map<String, Map<String, List<String>>> schedule = makeSchedule(employees, 42L);
        validateSchedule(employees, schedule);
        printSchedule(schedule, employees);
        scanner.close();
    }
}
