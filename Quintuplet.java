class Pair<A, B> {
    private final A first;
    private final B second;

    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }

    public A getFirst() {
        return first;
    }

    public B getSecond() {
        return second;
    }

    // You might also override equals(), hashCode(), and toString()
}

// class for lsp messages
class Triplet<A, B, C> {
    private final A first;
    private final B second;
    private final C third;

    public Triplet(A first, B second, C third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public A getFirst()  { return first; }
    public B getSecond() { return second; }
    public C getThird()  { return third; }

    @Override
    public String toString() {
        return first + "" + second + "" +  "" + third;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Triplet<?, ?, ?> t)) return false;
        return java.util.Objects.equals(first, t.first)
            && java.util.Objects.equals(second, t.second)
            && java.util.Objects.equals(third, t.third);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(first, second, third);
    }
}


class Quadruplet<A, B, C, D> {
    private final A first;
    private final B second;
    private final C third;
    private final D fourth;

    public Quadruplet(A first, B second, C third, D fourth) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
    }

    public A getFirst()  { return first; }
    public B getSecond() { return second; }
    public C getThird()  { return third; }
    public D getFourth() { return fourth; }

    @Override
    public String toString() {
        return first + "" + second + "" +  "" + third + "" + fourth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Quadruplet<?, ?, ?, ?> q)) return false;
        return java.util.Objects.equals(first, q.first)
            && java.util.Objects.equals(second, q.second)
            && java.util.Objects.equals(third, q.third)
            && java.util.Objects.equals(fourth, q.fourth);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(first, second, third, fourth);
    }
}

class Quintuplet<A, B, C, D, E> {
    public A first;
    public B second;
    public C third;
    public D fourth;
    public E fifth;

    // Constructor
    public Quintuplet(A first, B second, C third, D fourth, E fifth) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
        this.fifth = fifth;
    }

    // String representation
    @Override
    public String toString() {
        return first + "" + second + "" +  "" + third + "" + fourth + "" + fifth;
    }

    // Example getters (optional)
    public A getFirst() { return first; }
    public B getSecond() { return second; }
    public C getThird() { return third; }
    public D getFourth() { return fourth; }
    public E getFifth() { return fifth; }
}

// class DecimalToBinaryBuiltIn {
//     public static void main(String[] args) {
//         int decimalNumber = 25;
//         String binaryResult = Integer.toBinaryString(decimalNumber);
//         System.out.println("Decimal " + decimalNumber + " in binary is: " + binaryResult); // Output: Decimal 25 in binary is: 11001
//    }
// }

// import java.util.ArrayList;
// import java.util.List;

// public static int[][] buildAdjacencyMatrix(List<String> lines) {
//     int n = lines.size() + 1;  // Total nodes
//     int[][] matrix = new int[n][n];

//     // Initialize with -1 everywhere, 0 on diagonal
//     for (int i = 0; i < n; i++) {
//         Arrays.fill(matrix[i], -1);
//         matrix[i][i] = 0;
//     }

//     // Fill upper triangle and mirror to lower
//     for (int i = 0; i < lines.size(); i++) {
//         String line = lines.get(i).trim();
//         if (line.isEmpty()) continue;

//         String[] tokens = line.split("\\s+");
//         for (int j = 0; j < tokens.length; j++) {
//             int cost = Integer.parseInt(tokens[j]);
//             int col = i + j + 1; // position in upper triangle
//             if (col < n && cost > 0) {
//                 matrix[i][col] = cost;
//                 matrix[col][i] = cost; // make symmetric
//             }
//         }
//     }

//     return matrix;
// }

// import java.util.List;

// public class ListAccessExample {
//     public static void main(String[] args) {
//         // Create a List of Strings
//         List<String> myStringList = new ArrayList<>();

//         // Add elements to the list
//         myStringList.add("Apple");
//         myStringList.add("Banana");
//         myStringList.add("Cherry");
//         myStringList.add("Date");

//         // Access the element at index 2 (which is the 3rd element)
//         int indexToAccess = 2;
//         String element = myStringList.get(indexToAccess);

//         System.out.println("The element at index " + indexToAccess + " is: " + element);

//         // Attempting to access an index out of bounds will result in an IndexOutOfBoundsException
//         // For example, trying to access index 4 in a list of size 4 (indices 0-3)
//         // int invalidIndex = 4;
//         // String invalidElement = myStringList.get(invalidIndex); // This line would throw an exception
//     }
// }