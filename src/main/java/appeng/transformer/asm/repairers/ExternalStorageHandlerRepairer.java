package appeng.transformer.asm.repairers;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import appeng.transformer.asm.InterfaceMethodRepairer;

public class ExternalStorageHandlerRepairer extends InterfaceMethodRepairer {

    static final String methCH = "canHandle";
    static final String descCHNew = "(Lnet/minecraft/tileentity/TileEntity;Lnet/minecraftforge/common/util/ForgeDirection;Lappeng/api/storage/IStorageChannel;Lappeng/api/networking/security/BaseActionSource;)Z";
    static final String descCHOld = "(Lnet/minecraft/tileentity/TileEntity;Lnet/minecraftforge/common/util/ForgeDirection;Lappeng/api/storage/StorageChannel;Lappeng/api/networking/security/BaseActionSource;)Z";
    static final String methGI = "getInventory";
    static final String descGINew = "(Lnet/minecraft/tileentity/TileEntity;Lnet/minecraftforge/common/util/ForgeDirection;Lappeng/api/storage/IStorageChannel;Lappeng/api/networking/security/BaseActionSource;)Lappeng/api/storage/IMEInventory;";
    static final String descGIOld = "(Lnet/minecraft/tileentity/TileEntity;Lnet/minecraftforge/common/util/ForgeDirection;Lappeng/api/storage/StorageChannel;Lappeng/api/networking/security/BaseActionSource;)Lappeng/api/storage/IMEInventory;";

    boolean hasCH = false;
    boolean hasGI = false;

    public ExternalStorageHandlerRepairer(String className) {
        super(className);
    }

    @Override
    public void visitMethod(String name, String desc) {
        if (methCH.equals(name) && descCHNew.equals(desc)) {
            hasCH = true;
        }
        if (methGI.equals(name) && descGINew.equals(desc)) {
            hasGI = true;
        }
    }

    @Override
    public void generateMethods(ClassVisitor cv) {
        if (!hasCH) {
            MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, methCH, descCHNew, null, null);
            mv.visitCode();
            Label labelElse = new Label();
            Label labelEnd = new Label();
            // if (channel instanceof StorageChannel)
            mv.visitVarInsn(Opcodes.ALOAD, 3);
            mv.visitTypeInsn(Opcodes.INSTANCEOF, "appeng/api/storage/StorageChannel");
            mv.visitJumpInsn(Opcodes.IFEQ, labelElse);
            // {
            // StorageChannel c = (StorageChannel) channel;
            mv.visitVarInsn(Opcodes.ALOAD, 3);
            mv.visitTypeInsn(Opcodes.CHECKCAST, "appeng/api/storage/StorageChannel");
            mv.visitVarInsn(Opcodes.ASTORE, 5);
            // return this.canHandle(te, d, c, mySrc);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitVarInsn(Opcodes.ALOAD, 5);
            mv.visitVarInsn(Opcodes.ALOAD, 4);
            invokeMethod(mv, thisClass, methCH, descCHOld);
            // } else 
            mv.visitJumpInsn(Opcodes.GOTO, labelEnd);
            // {
            mv.visitLabel(labelElse);
            // return false;
            mv.visitInsn(Opcodes.ICONST_0);
            // }
            mv.visitLabel(labelEnd);
            // Actual return
            mv.visitInsn(Opcodes.IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        if (!hasGI) {
            MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, methGI, descGINew, null, null);
            mv.visitCode();
            Label labelElse = new Label();
            Label labelEnd = new Label();
            // if (channel instanceof StorageChannel)
            mv.visitVarInsn(Opcodes.ALOAD, 3);
            mv.visitTypeInsn(Opcodes.INSTANCEOF, "appeng/api/storage/StorageChannel");
            mv.visitJumpInsn(Opcodes.IFEQ, labelElse);
            // {
            // StorageChannel c = (StorageChannel) channel;
            mv.visitVarInsn(Opcodes.ALOAD, 3);
            mv.visitTypeInsn(Opcodes.CHECKCAST, "appeng/api/storage/StorageChannel");
            mv.visitVarInsn(Opcodes.ASTORE, 5);
            // return this.getInventory(te, d, c, mySrc);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitVarInsn(Opcodes.ALOAD, 5);
            mv.visitVarInsn(Opcodes.ALOAD, 4);
            invokeMethod(mv, thisClass, methGI, descGIOld);
            // } else 
            mv.visitJumpInsn(Opcodes.GOTO, labelEnd);
            // {
            mv.visitLabel(labelElse);
            // return null;
            mv.visitInsn(Opcodes.ACONST_NULL);
            // }
            mv.visitLabel(labelEnd);
            // Actual return
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        if ("appeng/api/storage/IExternalStorageHandler".equals(thisClass)) {
            {
                MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, methCH, descCHOld, null, null);
                mv.visitCode();
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitVarInsn(Opcodes.ALOAD, 2);
                mv.visitVarInsn(Opcodes.ALOAD, 3);
                mv.visitVarInsn(Opcodes.ALOAD, 4);
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, thisClass, methCH, descCHNew, true);
                mv.visitInsn(Opcodes.IRETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
            {
                MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, methGI, descGIOld, null, null);
                mv.visitCode();
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitVarInsn(Opcodes.ALOAD, 2);
                mv.visitVarInsn(Opcodes.ALOAD, 3);
                mv.visitVarInsn(Opcodes.ALOAD, 4);
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, thisClass, methGI, descGINew, true);
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
        }
    }
    
}
