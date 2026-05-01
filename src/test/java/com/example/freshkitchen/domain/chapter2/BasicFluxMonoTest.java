package com.example.freshkitchen.domain.chapter2;

import org.jetbrains.annotations.Async;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class BasicFluxMonoTest {

    @Test
    public void testBasicFlux() {

        Flux.<Integer>just(1, 2, 3, 4, 5)
                .map(data -> data * 2)
                .filter(data -> data % 4 == 0)
                .subscribe(data -> System.out.println("data = " + data));


        Mono.<Integer>just(1)
                .map(d -> d * 2)
                .filter(d -> d % 2 == 0)
                .subscribe(s -> System.out.println("s=" + s));
        Function<String, Function<String, String>> greeting = (greetingText) -> {
            return (name) -> {
                return greetingText + " " + name;
            };
        };

        Function<String, String> hello = greeting.apply("Hello");
        Function<String, String> hi = greeting.apply("HI");

        hello.apply("하준");
        System.out.println(hello.apply("무명소졸"));
        System.out.println(hi.apply("무명소졸"));
        System.out.println(hello.apply("하준"));
    }

    @Test
    void basicTest() {

        Mono.just(1).subscribe(s -> System.out.println("s = " + s));
        Mono.empty().subscribe(t -> System.out.println("t = " + t));


        Mono<String> stringMono = Mono.fromCallable(() -> {

            return "안녕";
        }).subscribeOn(Schedulers.boundedElastic());
        System.out.println("stringMono = " + stringMono);

    }


    @Test
    public void testFluxFromDate() {
        Flux.just(1, 2, 3, 4)
                .subscribe(d -> System.out.println("d = " + d));
        List<Integer> basicList = List.of(1, 2, 3, 4);
        Flux.fromIterable(basicList)
                .subscribe(data -> System.out.println("data fromIterable = " + data));

    }

    @Test
    public void testFluxFromFunction() {
        Flux.defer(() -> {
            return Flux.just(12, 3, 4, 5);
        }).subscribe(data -> System.out.println("data = " + data));


        Flux.create(sink -> {
            sink.next(1);
            sink.next(2);
            sink.next(3);
            sink.next(4);
            sink.complete();
        }).subscribe(data -> System.out.println("data = " + data));


    }

    @Test
    public void testSinkDetail() {
        Flux.<String>create(sink ->
        {
            AtomicInteger counter = new AtomicInteger(0);
            recursive(sink, counter);
        }).subscribe(d -> System.out.println("d = " + d));
    }

    // 재귀함수에서 방출하는 게  다다 .
    public void recursive(FluxSink<String> sink, AtomicInteger counter) {
        if (counter.incrementAndGet() < 10) {
            sink.next("sink count : " + counter);
            recursive(sink, counter);
        } else {
            sink.complete();
        }
    }


    @Test
    public void testFlux() {
        Flux.just(1, 2, 3, 4, 5)
                .map(i -> i * 2)
                .filter(d -> d % 4 == 0)
                .subscribe(data -> System.out.println("data = " + data));
    }


    @Test
    public void testFluxCollectList2() {
        Mono<List<Integer>> listMono = Flux.just(1, 2, 3, 4, 5)
                .map(d -> d * 2)
                .filter(d -> d % 4 == 0)
                .collectList();

        listMono.subscribe(data -> System.out.println("collect List 가 반환한 list data" + data));


    }


    @Test
    public void testWebClientMap(){

        // 중첩된 데이터 구조
        Flux<String> stringFlux = Flux.just(callWebClient("1단계 - 문제 이해하기", 1500),
                        callWebClient("2단계 - 문제 단계별로 풀어가기 ", 1000),
                        callWebClient("3단계 - 최종 응답 ", 500))
                .flatMapSequential(data -> {
                    return data;
                });
        stringFlux.subscribe(d-> System.out.println("flatMapSequential data = " + d));

        // 반환객체를 명시 해 주어야 한다.
        Flux<String> stringFlux2= Flux.merge(
            callWebClient("1단계 - 문제 이해하기", 1500),
           callWebClient("2단계 - 문제 단계별로 풀어가기 ", 1000),
           callWebClient("3단계 - 최종 응답 ", 500)
        );

        stringFlux2.subscribe(s-> System.out.println("s = " + s));


        StepVerifier.create(stringFlux)
                .expectNextCount(3)
                .verifyComplete();



    }

    public Mono<String> callWebClient(String request, long delay) {
        return Mono.defer(() -> {
            try {
                Thread.sleep(delay);
                return Mono.just(request + " -> 딜레이" + delay);
            } catch (Exception e) {
                return Mono.empty();
            }
        }).subscribeOn(Schedulers.boundedElastic());

    }


    @Test
    public void testFluxMonoError(){
        Flux.just(1,2,3,5)
                .flatMap(data->{
                    if(data!=3){
                        return Mono.just(data);
                    }else{
                        return Mono.error(new RuntimeException());
                        // throw new RunttimeException
                    }
                }).subscribe(data-> System.out.println("data = " + data));
    }
}
