package org.mozilla.javascript.classy;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class TestInlineCaching extends ClassyScriptable{
	
	
	static TestInlineCaching t0 = null,t1=null, t2=null;
	static Map<String,Integer> hm;
	ClassyLayout possibleType = null;
	int possibleOffset;
	
	static{

		t0 = new TestInlineCaching();
		t0.put("x", null, 100);
		t0.put("y", null, 200);
		t0.put("z", null, 300);
		
		t1 = new TestInlineCaching();
		t1.put("x",null,10);
		t1.put("p", null, 20);
		
		t2 = new TestInlineCaching();
		t2.put("name", null, 500);
		
		hm = new HashMap<String, Integer>();
		hm.put("x",10);
		hm.put("p",20);
		
	}
	

	public static void main(String[] args) {

		int value = (Integer) t0.getWithInlineCaching("x");
		int value1 = (Integer)t0.get("x",null);
		System.out.println(value);
		System.out.println(value1);
		System.out.println(t1.getWithInlineCaching("p"));
		System.out.println(t1.getWithInlineCaching("x"));

		long time = 0;

		for(int  i=0;i<10;i++){
			time = System.currentTimeMillis();
			for(int j=0;j<10000000;j++){
				t1.get("x");
			}
			System.out.print(System.currentTimeMillis() - time+"\t");
		}
		
		System.out.println("");
		
		for(int  i=0;i<10;i++){
			time = System.currentTimeMillis();
			for(int j=0;j<10000000;j++){
				t1.getWithInlineCaching("x");
			}
			System.out.print(System.currentTimeMillis() - time+"\t");
		}
		
		System.out.println();
		
		for(int  i=0;i<10;i++){
			time = System.currentTimeMillis();
			for(int j=0;j<10000000;j++){
				hm.get("x");
			}
			System.out.print(System.currentTimeMillis() - time+"\t");
		}
		
	}


	private Object getWithInlineCaching(String key) {

		if(this.possibleType == null){
			Map<String,Object> typeInformatioMap = extendedGet(key, null);
			this.possibleType = (ClassyLayout)typeInformatioMap.get("type");
			this.possibleOffset = (Integer)typeInformatioMap.get("offset");
			return typeInformatioMap.get("value");
		}else{
			ClassyLayout cuurentType = this.getTypeOfObject();
			if(cuurentType == this.possibleType){
				this.possibleOffset = this.getoffset(key);
				return this.getValueAtOffset(this.possibleOffset);
			}else{
				this.possibleType = null;
				return getWithInlineCaching(key);
			}
		}
	
	}
	


}
