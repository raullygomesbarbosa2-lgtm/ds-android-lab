package lab.ds.core

/** Original ARM-state CPU experiment. This narrow subset is not a DS CPU or game-compatible emulator. */
class Arm9Subset(val memory: ByteArray = ByteArray(1 shl 20)) {
    val r = IntArray(16)
    var cpsr = 0

    private fun check(a: Int, n: Int) { require(a >= 0 && a <= memory.size - n) { "test RAM access outside bounds: $a" } }
    private fun u8(a: Int) = (memory[a].toInt() and 255)
    private fun read32(a: Int): Int { check(a, 4); return u8(a) or (u8(a+1) shl 8) or (u8(a+2) shl 16) or (u8(a+3) shl 24) }
    private fun write32(a: Int, v: Int) { check(a, 4); for (i in 0..3) memory[a+i] = (v ushr (i*8)).toByte() }
    fun load(words: IntArray, address: Int = 0) { words.forEachIndexed { i,w -> write32(address+i*4,w) }; r[15]=address }
    private fun bit(n: Int) = cpsr ushr n and 1 != 0
    private fun reg(n: Int, pc: Int) = if (n == 15) pc+8 else r[n]
    private fun cond(c: Int): Boolean {
        val n=bit(31); val z=bit(30); val carry=bit(29); val v=bit(28)
        return when(c) { 0->z; 1->!z; 2->carry; 3->!carry; 4->n; 5->!n; 6->v; 7->!v; 8->carry&&!z; 9->!carry||z; 10->n==v; 11->n!=v; 12->!z&&n==v; 13->z||n!=v; 14->true; else->throw UnsupportedOperationException("NV condition") }
    }
    private data class Op(val value:Int,val carry:Boolean)
    private fun operand(ins:Int, pc:Int): Op {
        if (ins ushr 25 and 1 != 0) {
            val imm=ins and 255; val rot=(ins ushr 8 and 15)*2
            val value=Integer.rotateRight(imm,rot); return Op(value, if(rot==0) bit(29) else value<0)
        }
        if (ins and 0x10 != 0) throw UnsupportedOperationException("register-controlled shift")
        val value=reg(ins and 15,pc); val amount=ins ushr 7 and 31
        return when(ins ushr 5 and 3) {
            0 -> if(amount==0) Op(value,bit(29)) else Op(value shl amount, value ushr (32-amount) and 1 != 0)
            1 -> if(amount==0) Op(0,value<0) else Op(value ushr amount,value ushr (amount-1) and 1 != 0)
            2 -> if(amount==0) Op(if(value<0) -1 else 0,value<0) else Op(value shr amount,value shr (amount-1) and 1 != 0)
            else -> if(amount==0) Op((if(bit(29)) Int.MIN_VALUE else 0) or (value ushr 1),value and 1 != 0) else { val x=Integer.rotateRight(value,amount); Op(x,x<0) }
        }
    }
    private fun nz(x:Int) { cpsr=(cpsr and 0x3fffffff) or (if(x<0) Int.MIN_VALUE else 0) or (if(x==0) 1 shl 30 else 0) }
    private fun flags(a:Int,b:Int,result:Int,sub:Boolean,carryIn:Int=0) {
        nz(result)
        val wide=if(sub) (a.toLong() and 0xffffffffL)-(b.toLong() and 0xffffffffL)-carryIn else (a.toLong() and 0xffffffffL)+(b.toLong() and 0xffffffffL)+carryIn
        val c=if(sub) wide>=0 else wide ushr 32 != 0L
        val v=if(sub) ((a xor b) and (a xor result))<0 else ((a xor result) and (b xor result))<0
        cpsr=(cpsr and 0x0fffffff) or (if(c) 1 shl 29 else 0) or (if(v) 1 shl 28 else 0)
    }
    fun step(): Int {
        val pc=r[15]; val ins=read32(pc); r[15]=pc+4
        if(!cond(ins ushr 28)) return 1
        if(ins ushr 25 and 7==5) { val off=(ins shl 8) shr 6; if(ins ushr 24 and 1 != 0) r[14]=pc+4; r[15]=pc+8+off; return 3 }
        if(ins ushr 26 and 3==1) {
            if(ins ushr 25 and 1 != 0) throw UnsupportedOperationException("register-offset transfer")
            val rn=ins ushr 16 and 15; val rd=ins ushr 12 and 15; val base=reg(rn,pc); val off=ins and 0xfff
            val adjusted=if(ins ushr 23 and 1 != 0) base+off else base-off
            val pre=ins ushr 24 and 1 != 0; val addr=if(pre) adjusted else base
            if(ins ushr 22 and 1 != 0) throw UnsupportedOperationException("byte transfer")
            if(ins ushr 20 and 1 != 0) r[rd]=read32(addr) else write32(addr,reg(rd,pc))
            if(!pre || ins ushr 21 and 1 != 0) r[rn]=adjusted
            return 2
        }
        if(ins ushr 26 and 3==0) {
            val op=ins ushr 21 and 15; val set=ins ushr 20 and 1 != 0; val rn=reg(ins ushr 16 and 15,pc); val rd=ins ushr 12 and 15; val sh=operand(ins,pc); val b=sh.value
            val result=when(op) { 0,8->rn and b; 1,9->rn xor b; 2,10->rn-b; 3->b-rn; 4,11->rn+b; 5->rn+b+(if(bit(29)) 1 else 0); 6->rn-b-(if(bit(29)) 0 else 1); 7->b-rn-(if(bit(29)) 0 else 1); 12->rn or b; 13->b; 14->rn and b.inv(); 15->b.inv(); else->throw UnsupportedOperationException("data opcode $op") }
            val test=op in 8..11
            if(!test) { if(rd==15) throw UnsupportedOperationException("PC destination"); r[rd]=result }
            if(set || test) {
                when(op) {
                    2,10 -> flags(rn,b,result,true)
                    3 -> flags(b,rn,result,true)
                    4,11 -> flags(rn,b,result,false)
                    5 -> flags(rn,b,result,false,if(bit(29)) 0 else 1)
                    6 -> flags(rn,b,result,true,if(bit(29)) 0 else 1)
                    7 -> flags(b,rn,result,true,if(bit(29)) 0 else 1)
                    else -> { nz(result); cpsr=(cpsr and 0x1fffffff) or (if(sh.carry) 1 shl 29 else 0) }
                }
            }
            return 1
        }
        throw UnsupportedOperationException("ARM instruction 0x${ins.toUInt().toString(16)} at 0x${pc.toUInt().toString(16)}")
    }
    fun run(maxSteps:Int=10000):Int { var n=0; while(n<maxSteps) { step(); n++ }; return n }
}
