package dev.ronova.coremod.observation;

import java.util.List;

public final class NbtWriteReferencesSelfTest {
    public static void main(String[] args)throws Exception {
        Object root=new Object(),list=new Object(),first=new Object(),second=new Object();
        var trace=new NbtWriteReferences(root);
        try(var parent=trace.enter(root,list,"items",false,true)) {
            try(var item=trace.enter(list,first,null,true,true)){}
            try(var item=trace.enter(list,second,null,true,true)){}
        }
        var snapshot=trace.snapshot();require(snapshot.complete()&&snapshot.entries().size()==2,"actual traversal incomplete");
        require(snapshot.entries().get(1).value().get()==second&&snapshot.entries().get(1).path().equals(List.of(
            new NbtStoragePreservation.Key("items"),new NbtStoragePreservation.Index(1))),"actual list identity/index lost");
        var wrong=new NbtWriteReferences(root);wrong.enter(list,first,"key",true,true);require(!wrong.snapshot().complete(),"wrong parent accepted");
        var unsupported=new NbtWriteReferences(root);unsupported.enter(root,first,"key",true,false);require(!unsupported.snapshot().complete(),"custom writer accepted");
        var cycle=new NbtWriteReferences(root);cycle.enter(root,root,"key",true,true);require(!cycle.snapshot().complete(),"cycle accepted");
        var overflow=new NbtWriteReferences(root);for(int i=0;i<65;i++)try(var item=overflow.enter(root,new Object(),"k"+i,true,true)){}
        require(!overflow.snapshot().complete()&&overflow.snapshot().entries().isEmpty(),"partial coverage escaped");
        var released=new NbtWriteReferences(root);var late=released.enter(root,first,"item",true,true);
        released.release();late.close();released.release();
        require(!released.snapshot().complete()&&released.snapshot().entries().isEmpty()
            &&released.enter(root,second,"late",true,true)==null,"released traversal accepted late evidence");
        require(snapshot.entries().get(1).value().get()==second,"release changed previously published independent evidence");
        System.out.println("NbtWriteReferencesSelfTest PASS");
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
