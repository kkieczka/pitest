package org.pitest.mutationtest.engine.gregor.mutators.experimental;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.MutationContext;

/**
 * Mutator which changes default values of JSONObject.opt*() method calls.
 *
 * Created by Krystian Kieczka on 2018-02-15.
 */
public enum JSONChangeDefaultValueMutator implements MethodMutatorFactory {
    JSON_CHANGE_DEFAULT_VALUE;

    public class JSONChangeDefaultValueMethodVisitor extends MethodVisitor {

        private MutationContext context;
        private MethodMutatorFactory factory;

        public JSONChangeDefaultValueMethodVisitor(MethodMutatorFactory factory,
                                                   MutationContext context, MethodVisitor mv) {
            super(Opcodes.ASM6, mv);
            this.context = context;
            this.factory = factory;
        }

        private boolean isOptMethod(String owner, String name) {
            return "org/json/JSONObject".equals(owner) && name.startsWith("opt")
                    && !name.equals("opt") && !name.equals("optJSONArray")
                    && !name.equals("optJSONObject");
        }

        private boolean doMutation(MutationIdentifier id, int pop, Object parameter, int opcode,
                                String owner, String name, String desc, boolean itf) {
            if (this.context.shouldMutate(id)) {
                if (pop == 1) {
                    this.mv.visitInsn(Opcodes.POP);
                } else if (pop == 2) {
                    this.mv.visitInsn(Opcodes.POP2);
                }
                this.mv.visitLdcInsn(parameter);
                this.mv.visitMethodInsn(opcode, owner, name, desc, itf);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {

            if (isOptMethod(owner, name)) {
                Object newValue;
                String newMethodDesc;
                int pop = 0;
                switch (desc) {

                    case "(Ljava/lang/String;Z)Z":
                        pop = 1;
                    case "(Ljava/lang/String;)Z":
                        newValue = true;
                        newMethodDesc = "(Ljava/lang/String;Z)Z";
                        break;

                    case "(Ljava/lang/String;D)D":
                        pop = 2;
                    case "(Ljava/lang/String;)D":
                        newValue = 12345.67d;
                        newMethodDesc = "(Ljava/lang/String;D)D";
                        break;

                    case "(Ljava/lang/String;I)I":
                        pop = 1;
                    case "(Ljava/lang/String;)I":
                        newValue = 1234567;
                        newMethodDesc = "(Ljava/lang/String;I)I";
                        break;

                    case "(Ljava/lang/String;J)J":
                        pop = 2;
                    case "(Ljava/lang/String;)J":
                        newValue = 1234567L;
                        newMethodDesc = "(Ljava/lang/String;J)J";
                        break;

                    case "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;":
                        pop = 1;
                    case "(Ljava/lang/String;)Ljava/lang/String;":
                        newValue = "qwerty";
                        newMethodDesc = "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;";
                        break;

                    default:
                        // unknown version of opt* function
                        super.visitMethodInsn(opcode, owner, name, desc, itf);
                        return;
                }
                MutationIdentifier newId = this.context.registerMutation(
                        this.factory, "Changed default value of " + name + " to " + newValue);

                if (!doMutation(newId, pop, newValue, opcode, owner, name, newMethodDesc, itf)) {
                    super.visitMethodInsn(opcode, owner, name, desc, itf);
                }
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }

        }
    }

    @Override
    public MethodVisitor create(MutationContext context, MethodInfo methodInfo, MethodVisitor methodVisitor) {
        return new JSONChangeDefaultValueMethodVisitor(this, context, methodVisitor);
    }

    @Override
    public String getGloballyUniqueId() {
        return getName();
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}
