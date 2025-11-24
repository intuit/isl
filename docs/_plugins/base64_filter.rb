require 'base64'

module Jekyll
  module Base64Filter
    # Encodes a string to base64 URL-safe format
    # Replaces + with -, / with _, and removes = padding
    def base64_url_encode(input)
      return '' if input.nil? || input.empty?
      
      # Convert to base64
      base64 = Base64.strict_encode64(input.to_s)
      
      # Make it URL-safe
      base64.tr('+/', '-_').gsub(/=+$/, '')
    end
    
    # Decodes a base64 URL-safe string
    def base64_url_decode(input)
      return '' if input.nil? || input.empty?
      
      # Convert URL-safe base64 back to standard base64
      base64 = input.tr('-_', '+/')
      
      # Add padding if needed
      padding = (4 - base64.length % 4) % 4
      base64 += '=' * padding
      
      # Decode
      Base64.strict_decode64(base64)
    rescue ArgumentError => e
      # Return empty string if decoding fails
      Jekyll.logger.warn "Base64Filter:", "Failed to decode base64: #{e.message}"
      ''
    end
  end
end

Liquid::Template.register_filter(Jekyll::Base64Filter)

