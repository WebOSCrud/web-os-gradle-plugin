public class Test {
    public static void main(String[] args) throws InterruptedException {
        double progress=0;
        long time=System.currentTimeMillis();
        while (progress<=100) {
            Thread.sleep(10);
            if(System.currentTimeMillis()-time>100){
                System.out.printf("\rDownload progress: %.2f%%", progress);
                time=System.currentTimeMillis();
            }
            progress+=0.1;
        }
    }
}
