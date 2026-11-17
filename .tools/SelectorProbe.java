import java.nio.channels.Selector;
class SelectorProbe { public static void main(String[] a) throws Exception { try (var s = Selector.open()) { System.out.println(s.getClass()); } } }
