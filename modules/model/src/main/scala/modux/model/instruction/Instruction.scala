package modux.model.instruction

trait Instruction[COMMAND, EFFECT] {

  def apply(effect: EFFECT, command: COMMAND): EFFECT

  def apply(effect: EFFECT, commands: Seq[COMMAND]): EFFECT = {
    commands.foldLeft(effect) { case (current, item) => this (current, item) }
  }
}
