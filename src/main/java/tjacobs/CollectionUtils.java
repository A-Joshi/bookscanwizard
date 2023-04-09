/*
 * Created on Nov 6, 2004 by @author Tom Jacobs
 *
 */
package tjacobs;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CollectionUtils is mostly utility methods for printing collections
 */
public class CollectionUtils {

	//not instantiable
	private CollectionUtils() {
	}
	
	/** Get an element from a list which equals the parameter */
	@SuppressWarnings("unchecked")
	public static Object get(List l, Object o) {
		Iterator i = l.iterator();
		Object obj = i.next();
		if (obj.equals(o)) {
			return obj;
		}
		return null;
	}
	
	/**
	 * Print the Key/Pair mappings contained in map m
	 * @param m
	 */
	@SuppressWarnings("unchecked")
	public static void printCollection(Map m) {
		printCollection(m, false, System.out);
	}

	/**
	 * Print the Key/Pair mappings contained in map m
	 * @param m
	 */
	@SuppressWarnings("unchecked")
	public static void printCollection(Map m, boolean sameLine, PrintStream out) {
		if (m == null) {
			System.out.println("null map");
			return;
		}
		Iterator i = m.keySet().iterator();
		while (i.hasNext()) {
			Object key = i.next();
			Object value = m.get(key);
			if (key == null) {
				key = "null";
			}
			if (value == null) {
				value = "null";
			}
			if (sameLine) {
				out.print(key.toString() + " => " + value.toString() + " ");
			}
			else {
				out.println(key.toString() + " => " + value.toString());
			}
		}
		if (sameLine) out.println();
	}
	
	/**
	 * Print the objects contained in list l
	 * @param l
	 */
	@SuppressWarnings("unchecked")
	public static void printCollection(List l) {
		printCollection(l, false, System.out);
	}
		
	/**
	 * Print the objects contained in list l
	 * @param l
	 */
	@SuppressWarnings("unchecked")
	public static void printCollection(List l, boolean sameLine, PrintStream out) {
		if (l == null) {
			out.println("null list");
			return;
		}
		Iterator i = l.iterator();
		while (i.hasNext()) {
			Object val = i.next();
			if (sameLine) out.print(val.toString()); 
				else out.println(val.toString() + " ");
		}
		if (sameLine) out.println();
	}
	
	/**
	 * Print the objects contained in set s
	 * @param s
	 */
	@SuppressWarnings("unchecked")
	public static void printCollection(Set s) {
		printCollection(s, true, System.out);
	}
		
	/**
	 * Print the objects contained in set s
	 * @param s
	 */
	@SuppressWarnings("unchecked")
	public static void printCollection(Set s, boolean sameLine, PrintStream out) {
		if (s == null) {
			System.out.println("null set");
			return;
		}
		Iterator i = s.iterator();
		while (i.hasNext()) {
			Object val = i.next();
			if (sameLine) {out.print(val.toString() + " ");} else {out.println(val.toString()); }
		}
		if (sameLine) out.println();
	}
	
	/**
	 * Concatenates strings into a StringBuffer
	 * @param strs
	 * @return
	 */
	public static StringBuffer printStringArray(String[] strs) {
		if (strs == null || strs.length == 0) {
			return new StringBuffer();
		}
		StringBuffer sb = new StringBuffer(strs[0]);
		for (int i = 1; i < strs.length; i++) {
			sb.append("\n");
			sb.append(strs[i]);
		}
		return sb;
	}
	
	/**
	 * Concatenates strings into a StringBuffer
	 * @param strs
	 * @return
	 */
	public static StringBuffer printStringArrayInline(String[] strs) {
		if (strs == null || strs.length == 0) {
			return new StringBuffer();
		}
		StringBuffer sb = new StringBuffer(strs[0]);		
		for (int i = 1; i < strs.length; i++) {
			sb.append(", ");
			sb.append(strs[i]);
		}
		return sb;
	}
		
	/**
	 * Prints an array of Objects
	 * @param strs
	 * @return
	 */
	public static void printCollection(Object[] objs) {
		if (objs == null) System.out.println("Collection is null");
		for (Object o : objs) {
			if (o == null) {
				System.out.println("null");
			}
			else System.out.println(o.toString());
		}
	}
		
	@SuppressWarnings("unchecked")
	public static List toList(Collection c) {
		if (c instanceof List) return (List)c;
		return toList(c.iterator());
	}
	
	@SuppressWarnings("unchecked")
	public static List toList(Iterator i) {
		List val = new ArrayList();
		while (i.hasNext()) {
			val.add(i.next());
		}
		return val;
	}
	
	/**
	 * Get all subsets in a recursive manner
	 * @param objs
	 * @param lengthIndex
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Set<Set> allSubsetsRecursive(Object[] objs, int lengthIndex) {
		Set<Set> vals = new HashSet<Set>();
		if (objs == null || lengthIndex < 0 || objs.length == 0) {
			HashSet val1 = new HashSet();
			vals.add(val1);
			return vals;
		}
		else {
			Set<Set> vals1 = allSubsetsRecursive(objs, lengthIndex - 1);
			Set<Set> vals2 = allSubsetsRecursive(objs, lengthIndex - 1);
			Iterator<Set> _j = vals2.iterator();
			while (_j.hasNext()) {
				Set s = _j.next();
				s.add(objs[lengthIndex]);
			}
			vals.addAll(vals1);
			vals.addAll(vals2);
			return vals;
		}
	}
	
	/**
	 * Get all subsets in an iterative manner
	 * @param objs
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<Set> getAllSubsets(Object[] objs) {
		ArrayList<Set> sets = new ArrayList<Set>();
		if (objs == null || objs.length == 0) {
			sets.add(new HashSet());
			return sets;
		}
		int len = objs.length;
		int total = (int) Math.pow(2, len);
		for (int i = 0; i < total; i++) {
			sets.add(new HashSet());
		}
		for (int i = 0; i < objs.length; i++) {
			int seg = (int)Math.pow(2, i);
			int numsegs = total / 2 / seg;
			for (int j = 0; j < numsegs; j++) {
				for (int k = 0; k < seg; k++) {
					int getSet = j * 2 * seg + k;
					Set set = sets.get(getSet);
					set.add(objs[i]);
				}
			}
		}
		return sets;
	}
	
	/**
	 * Get subsets of collection c
	 * @param c
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Set<Set> getAllSubsets(Collection c) {
		Iterator _i = c.iterator();
		int size = c.size();
		Object[] objs = new Object[size];
		for (int i = 0; i < size; i++) {
			objs[i] = _i.next();
		}
		Set<Set> sets = allSubsetsRecursive(objs, size - 1), sets2 = allSubsetsRecursive(objs, size - 1);
		Iterator<Set> _j = sets2.iterator();
		while (_j.hasNext()) {
			Set s = _j.next();
			s.add(objs[size - 1]);
		}
		sets.addAll(sets);
		return sets;
	}
	
	/**
	 * Iteratively generate all subsets
	 * @param <T>
	 * @param s
	 * @return
	 */
	public static <T> Set<Set<T>> powerSet(Set<T> s) {
		if (s.size() > 31) throw new IllegalArgumentException("too big");
		List<T> elts = new ArrayList<T>(s);
		Set<Set<T>> pow = new LinkedHashSet<Set<T>>();
		for(int bits = 0; bits < 1<<s.size(); ++bits) {
			Set<T> subset = new LinkedHashSet<T>();
			for(int bit=0; bit < s.size(); ++bit) {
				if ((bits & (1<<bit)) != 0) {
					subset.add(elts.get(bit));
				}
			}
			pow.add(subset);
		}
 
		return pow;
	}
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		String[] vals = new String[] {"A", "B", "C", "D", "E", "F"};
		HashSet<String> hs = new HashSet();
		for (int i = 0; i < vals.length; i++) {
			hs.add(vals[i]);
		}
		Set<Set> subs = getAllSubsets(hs);
		List<Set> subs2 = getAllSubsets(vals);
		Set<Set<String>> subs3 = powerSet(hs);
		printCollection(subs);
		System.out.println();
		printCollection(subs2);
		System.out.println("size = " + subs.size());
		System.out.println("size2 = " + subs2.size());
		printCollection(subs3);
		System.out.println("size3 = " + subs3.size());
	}
}