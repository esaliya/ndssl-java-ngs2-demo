package org.saliya.ndssl.ngs2demo.intersim;

/**
 * Saliya Ekanayake on 3/16/17.
 */
public class IntersimAgent {
    private int age;
    private int gender;

    public IntersimAgent(int age, int gender) {
        this.age = age;
        this.gender = gender;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getGender() {
        return gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }
}
