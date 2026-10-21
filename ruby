ruby {
  init => '
    @empty_strings = [
      "-", "", " ", "None", "UNKNOWN",
      "N/A", "NA", "(empty)"
    ].freeze

    def clean_hash(event, hash, parent)
      hash.each do |key, value|

        if value.is_a?(Hash)
          path = "#{parent}[#{key}]"

          clean_hash(event, value, path)

          item = event.get(path)
          event.remove(path) if item.is_a?(Hash) && item.empty?

        elsif value.nil? ||
              (value.is_a?(String) && @empty_strings.include?(value))

          event.remove("#{parent}[#{key}]")

        end
      end
    end
  '

  code => '
    clean_hash(event, event.to_hash, "")
  '
}
