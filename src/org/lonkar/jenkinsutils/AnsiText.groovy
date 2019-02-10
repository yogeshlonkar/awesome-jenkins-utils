package org.lonkar.jenkinsutils

@Grab('org.fusesource.jansi:jansi:1.17.1')
import org.fusesource.jansi.*
import static org.fusesource.jansi.Ansi.*
import java.io.Serializable

/**
 *
 * Utility class for generating Ansi text using Fluent interface.
 * Credits to https://github.com/fusesource/jansi.
 * However {@link Ansi} class somehow returns {@link NoAnsi} instance,
 * Which does not have all helper methods from {@link Ansi}.
 *
 * Will require <a href="https://plugins.jenkins.io/ansicolor">ansicolor</A> plugin.
 *
 */
public class AnsiText implements Serializable {

  private int tabToKeep

  private transient Ansi ansi

  public AnsiText() {
    this.ansi = new Ansi()
  }

  public AnsiText(Ansi parent) {
    this.ansi = new Ansi(parent)
  }

  public AnsiText(int size) {
    this.ansi = new Ansi(size)
  }

  public AnsiText(StringBuilder builder) {
    this.ansi = new Ansi(builder)
  }

  def AnsiText fg(Color color) {
    this.ansi.fg(color)
    return this
  }

  def AnsiText fgBlack() {
    this.ansi.fg(Color.BLACK)
    return this
  }

  def AnsiText fgBlue() {
    this.ansi.fg(Color.BLUE)
    return this
  }

  def AnsiText fgCyan() {
    this.ansi.fg(Color.CYAN)
    return this
  }

  def AnsiText fgDefault() {
    this.ansi.fg(Color.DEFAULT)
    return this
  }

  def AnsiText fgGreen() {
    this.ansi.fg(Color.GREEN)
    return this
  }

  def AnsiText fgMagenta() {
    this.ansi.fg(Color.MAGENTA)
    return this
  }

  def AnsiText fgRed() {
    this.ansi.fg(Color.RED)
    return this
  }

  def AnsiText fgYellow() {
    this.ansi.fg(Color.YELLOW)
    return this
  }

  def AnsiText bg(Color color) {
    this.ansi.bg(color)
    return this
  }

  def AnsiText bgCyan() {
    this.ansi.fg(Color.CYAN)
    return this
  }

  def AnsiText bgDefault() {
    this.ansi.bg(Color.DEFAULT)
    return this
  }

  def AnsiText bgGreen() {
    this.ansi.bg(Color.GREEN)
    return this
  }

  def AnsiText bgMagenta() {
    this.ansi.bg(Color.MAGENTA)
    return this
  }

  def AnsiText bgRed() {
    this.ansi.bg(Color.RED)
    return this
  }

  def AnsiText bgYellow() {
    this.ansi.bg(Color.YELLOW)
    return this
  }

  def AnsiText fgBrightBlack() {
    this.ansi.fgBright(Color.BLACK)
    return this
  }

  def AnsiText fgBrightBlue() {
    this.ansi.fgBright(Color.BLUE)
    return this
  }

  def AnsiText fgBrightCyan() {
    this.ansi.fgBright(Color.CYAN)
    return this
  }

  def AnsiText fgBrightDefault() {
    this.ansi.fgBright(Color.DEFAULT)
    return this
  }

  def AnsiText fgBrightGreen() {
    this.ansi.fgBright(Color.GREEN)
    return this
  }

  def AnsiText fgBrightMagenta() {
    this.ansi.fgBright(Color.MAGENTA)
    return this
  }

  def AnsiText fgBrightRed() {
    this.ansi.fgBright(Color.RED)
    return this
  }

  def AnsiText fgBrightYellow() {
    this.ansi.fgBright(Color.YELLOW)
    return this
  }

  def AnsiText bgBrightCyan() {
    this.ansi.fgBright(Color.CYAN)
    return this
  }

  def AnsiText bgBrightDefault() {
    this.ansi.bgBright(Color.DEFAULT)
    return this
  }

  def AnsiText bgBrightGreen() {
    this.ansi.bgBright(Color.GREEN)
    return this
  }

  def AnsiText bgBrightMagenta() {
    this.ansi.bg(Color.MAGENTA)
    return this
  }

  def AnsiText bgBrightRed() {
    this.ansi.bgBright(Color.RED)
    return this
  }

  def AnsiText bgBrightYellow() {
    this.ansi.bgBright(Color.YELLOW)
    return this
  }

  def AnsiText reset() {
    this.ansi.a(Attribute.RESET)
    return this
  }

  def AnsiText bold() {
    this.ansi.a(Attribute.INTENSITY_BOLD)
    return this
  }

  def AnsiText boldOff() {
    this.ansi.a(Attribute.INTENSITY_BOLD_OFF)
    return this
  }

  def AnsiText a(value) {
    this.ansi.a(value)
    return this
  }

  def AnsiText a(char[] value, int offset, int len) {
    this.ansi.a(value, offset, len)
    return this
  }

  def AnsiText a(CharSequence value, int start, int end) {
    this.ansi.a(value, start, end)
    return this
  }

  def AnsiText newline() {
    this.ansi.newline()
    this.tab(this.tabToKeep)
    return this
  }

  /**
   * Add tab in text
   *
   * @return
   */
  def AnsiText tab() {
    this.ansi.a("\t")
    return this
  }

  /**
   * Add tab n times in text
   *
   * @param n
   * @return
   */
  def AnsiText tab(int n) {
    while (n > 0) {
      this.ansi.a("\t")
        n--
    }
    return this
  }

  /**
   * Keep tab as a prefix to each new line n times in text every time new text is added
   *
   * @param n
   * @return
   */
  def AnsiText keepTabbed(int n) {
    this.tabToKeep = n
    return this
  }

  /**
   * reset tab prefix
   *
   * @return
   */
  def AnsiText resetTab() {
    this.tabToKeep = 0
    return this
  }

  /**
   * italicize text
   *
   * @return
   */
  def AnsiText italicize() {
    this.ansi.a(Attribute.ITALIC)
    return this
  }

  /**
   * turn of italics
   *
   * @return
   */
  def AnsiText italicizeOff() {
    this.ansi.a(Attribute.ITALIC_OFF)
    return this
  }

  /** @return */
  def AnsiText underline() {
    this.ansi.a(Attribute.UNDERLINE)
    return this
  }

  /** @return */
  def AnsiText underlineOff() {
    this.ansi.a(Attribute.UNDERLINE_OFF)
    return this
  }

  @Override
  def String toString() {
    this.reset()
    return this.ansi.toString()
  }

}
